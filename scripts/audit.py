#!/usr/bin/env python3
"""Tamekind codebase audit.

Mechanical checks for the specific mistakes this codebase actually makes, rather than a
generic linter. Every rule here exists because it shipped a real bug or a real piece of
sloppiness at least once. Same shape as the sibling mods' audits.

    python scripts/audit.py            # report
    python scripts/audit.py --quiet    # errors only

Exit code 1 if any ERROR is found. WARNINGs are judgement calls and never fail the run.
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
MAIN = ROOT / "src" / "main" / "java"
RESOURCES = ROOT / "src" / "main" / "resources"
GOALS = MAIN / "com" / "tamekind" / "ai" / "goal"
MIXINS = MAIN / "com" / "tamekind" / "mixin"
INJECTOR = MAIN / "com" / "tamekind" / "ai" / "PassiveGoalInjector.java"
PRIORITIES = MAIN / "com" / "tamekind" / "ai" / "GoalPriorities.java"
TAGS = MAIN / "com" / "tamekind" / "compat" / "TamekindTags.java"
CONFIG = MAIN / "com" / "tamekind" / "config" / "TamekindConfig.java"
MIXIN_JSON = RESOURCES / "tamekind.mixins.json"
FABRIC_JSON = RESOURCES / "fabric.mod.json"
LANG = RESOURCES / "assets" / "tamekind" / "lang" / "en_us.json"

# Spelled with chr() so this file does not trip its own check.
EM_DASH = chr(0x2014)

errors: list[str] = []
warnings: list[str] = []


def strip_comments_and_strings(src: str) -> str:
    """Crude but adequate: removes block/line comments and string literals."""
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"//[^\n]*", "", src)
    src = re.sub(r'"(?:\\.|[^"\\])*"', '""', src)
    return src


def java_files() -> list[pathlib.Path]:
    return sorted(MAIN.rglob("*.java"))


# ---------------------------------------------------------------- unused imports
def check_unused_imports() -> None:
    """An import left behind by an edit that deleted the last usage."""
    for f in java_files():
        src = f.read_text(encoding="utf-8")
        body = strip_comments_and_strings(re.sub(r"^import .*$", "", src, flags=re.M))
        for m in re.finditer(r"^import (?:static )?[\w.]+\.(\w+);$", src, re.M):
            name = m.group(1)
            if name == "*":
                continue
            if not re.search(r"\b" + re.escape(name) + r"\b", body):
                errors.append(f"unused import: {f.relative_to(ROOT)} -> {name}")


# ------------------------------------------------------------ unused private members
def check_unused_private_members() -> None:
    """Dead private helpers and constants read as live tuning and mislead the next reader."""
    for f in java_files():
        src = f.read_text(encoding="utf-8")
        body = strip_comments_and_strings(src)

        for m in re.finditer(r"private (?:static )?(?:final )?[\w<>\[\],. ?]+ (\w+)\s*\(", body):
            name = m.group(1)
            calls = len(re.findall(r"\b" + re.escape(name) + r"\s*\(", body))
            refs = len(re.findall(r"::\s*" + re.escape(name) + r"\b", body))
            if calls + refs <= 1:
                warnings.append(f"possibly unused private method: {f.relative_to(ROOT)} -> {name}()")

        for m in re.finditer(r"private static final [\w<>\[\],. ]+ ([A-Z][A-Z0-9_]*)\s*=", body):
            name = m.group(1)
            if len(re.findall(r"\b" + re.escape(name) + r"\b", body)) <= 1:
                errors.append(f"unused private constant: {f.relative_to(ROOT)} -> {name}")


# ------------------------------------------------------------------- mixin hygiene
def check_mixin_unique() -> None:
    """A tamekind$ prefix is convention; @Unique makes the compiler enforce it."""
    injector = re.compile(
        r"@(Inject|ModifyArg|ModifyArgs|ModifyVariable|ModifyReturnValue|Redirect"
        r"|WrapOperation|WrapWithCondition|Overwrite|Accessor|Invoker)")
    for f in sorted(MIXINS.rglob("*.java")):
        src = f.read_text(encoding="utf-8")
        for m in re.finditer(r"(?:@[\w()\"$., =\-]+\s+)*private [\w<>\[\],. ?]+ (\w+)\s*\(", src):
            name = m.group(1)
            preceding = src[max(0, m.start() - 400):m.start()]
            block = preceding.rsplit("}", 1)[-1]
            if injector.search(block) or "@Unique" in block:
                continue
            errors.append(
                f"mixin helper without @Unique: {f.relative_to(ROOT)} -> {name}() "
                f"(prefix alone is not enforced)")


def check_mixins_registered() -> None:
    """A mixin class not listed in tamekind.mixins.json never applies, silently."""
    if not MIXIN_JSON.exists():
        errors.append("mixins: tamekind.mixins.json is missing")
        return
    try:
        cfg = json.loads(MIXIN_JSON.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        errors.append(f"mixins: tamekind.mixins.json does not parse ({exc})")
        return

    listed = set()
    for bucket in ("mixins", "client", "server"):
        listed.update(cfg.get(bucket, []))
    on_disk = {f.stem for f in MIXINS.rglob("*.java")}
    for name in sorted(on_disk - listed):
        errors.append(f"mixins: {name} exists but is not listed in tamekind.mixins.json")
    for name in sorted(listed - on_disk):
        errors.append(f"mixins: tamekind.mixins.json lists {name}, which has no source file")

    if cfg.get("injectors", {}).get("defaultRequire") != 1:
        errors.append(
            "mixins: injectors.defaultRequire must stay 1 so a missing target is fatal at "
            "load rather than a mod that quietly does nothing")


def check_mixin_target_names() -> None:
    """Every @Inject method= is a string that has to match a real Minecraft method.

    defaultRequire: 1 makes a rename fatal at load, which is what we want, but it makes
    this list the porting checklist for every Minecraft update.
    """
    for f in sorted(MIXINS.rglob("*.java")):
        src = f.read_text(encoding="utf-8")
        target = re.search(r"@Mixin\((?:value\s*=\s*)?(\w+)\.class", src)
        owner = target.group(1) if target else "?"
        for m in re.finditer(r'method\s*=\s*"([\w$]+)', src):
            warnings.append(
                f"mixin target to re-verify on a Minecraft update: "
                f"{f.relative_to(ROOT)} -> {owner}#{m.group(1)}")
        for m in re.finditer(r'@Accessor\("(\w+)"\)', src):
            warnings.append(
                f"mixin accessor field to re-verify on a Minecraft update: "
                f"{f.relative_to(ROOT)} -> {owner}#{m.group(1)}")


def check_mixin_hardcoded_indexes() -> None:
    """@ModifyArg index= is a hand-written descriptor assumption, fatal at load if wrong."""
    for f in sorted(MIXINS.rglob("*.java")):
        src = f.read_text(encoding="utf-8")
        for m in re.finditer(r"index\s*=\s*(\d+)", src):
            warnings.append(
                f"hardcoded mixin arg index: {f.relative_to(ROOT)} -> index={m.group(1)} "
                f"(re-verify against the target descriptor on every MC update)")


# ------------------------------------------------------ reflection on MC member names
def check_no_minecraft_reflection() -> None:
    """Reflecting on a Minecraft member by name works in dev and fails when shipped.

    String literals are not remapped, so getDeclaredField("targetSelector") resolves in a
    dev run and throws NoSuchFieldException against the intermediary names a released jar
    runs on. WolfHuntMixin and FoxHuntMixin shipped exactly this, wrapped in
    catch (Throwable ignored), so wolves and foxes silently never hunted. Use an @Accessor
    mixin, which the mixin processor remaps for you.

    Reflecting into *another mod* is the opposite case and is how soft compat has to work:
    a foreign mod's own API names are not remapped either, and the class may be absent
    entirely. SereneSeasonsCompat is the sanctioned example. Those are reported as
    warnings, because the coupling is still worth seeing.
    """
    for f in java_files():
        src = f.read_text(encoding="utf-8")
        foreign = {
            m.group(1).split(".")[0]
            for m in re.finditer(r'Class\.forName\(\s*"([\w.]+)"', src)
            if not m.group(1).startswith("net.minecraft")
        }
        for m in re.finditer(r"\.(getDeclaredField|getField|getDeclaredMethod|getMethod)\(", src):
            line = src[: m.start()].count("\n") + 1
            member = m.group(1)
            # Field reflection has no legitimate use here: every Minecraft field this
            # codebase wants is reachable through an @Accessor.
            if "Field" in member or not foreign:
                errors.append(
                    f"reflection on a Minecraft member name: {f.relative_to(ROOT)}:{line} -> "
                    f"{member}() (string literals are not remapped; use an @Accessor mixin)")
        if foreign:
            warnings.append(
                f"soft-compat reflection: {f.relative_to(ROOT)} reflects into "
                f"{', '.join(sorted(foreign))} (fine, but re-check when that mod updates)")


# ------------------------------------------------------------------ porting hazards
def check_no_blockpos_getcenter() -> None:
    """BlockPos.getCenter() is gone in 26.2; Vec3.atCenterOf(pos) exists in both."""
    for f in java_files():
        src = strip_comments_and_strings(f.read_text(encoding="utf-8"))
        for m in re.finditer(r"\b\w+\.getCenter\(\)", src):
            line = src[: m.start()].count("\n") + 1
            errors.append(
                f"getCenter(): {f.relative_to(ROOT)}:{line} "
                f"(removed in 26.2; use Vec3.atCenterOf(pos), which works in both)")


def check_singular_tag_directories() -> None:
    """Datapack tag folders are singular since 1.21; a plural copy can never load.

    tags/blocks/ and tags/entity_types/ shipped as byte-identical dead copies of the real
    directories, which Minecraft ignored entirely and which would have drifted out of sync
    with the ones that do load.
    """
    data = RESOURCES / "data"
    if not data.is_dir():
        return
    for bad in ("blocks", "entity_types", "items", "fluids", "biomes"):
        for found in data.rglob(f"tags/{bad}"):
            errors.append(
                f"plural tag directory: {found.relative_to(ROOT).as_posix()} "
                f"(tag folders are singular since 1.21; this one can never load)")


# ---------------------------------------------------------------------- tag files
def check_tag_files_exist() -> None:
    """A TagKey whose datapack file is missing resolves to an empty tag, silently.

    Nothing throws and nothing logs; the behaviour the tag drives just stops applying.
    TamekindTags builds its keys through entity()/block() helpers, so the paths are read
    from those calls rather than from inline TagKey.create.
    """
    if not TAGS.exists():
        errors.append("tags: TamekindTags.java is missing")
        return
    src = TAGS.read_text(encoding="utf-8")
    for helper, registry in (("entity", "entity_type"), ("block", "block")):
        for m in re.finditer(helper + r'\("([\w/]+)"\)', src):
            path = m.group(1)
            expected = RESOURCES / "data" / "tamekind" / "tags" / registry / f"{path}.json"
            if not expected.exists():
                errors.append(
                    f"tag: TamekindTags uses #tamekind:{path} but "
                    f"{expected.relative_to(ROOT).as_posix()} does not exist "
                    f"(a missing tag resolves to empty and the behaviour quietly stops)")


# ------------------------------------------------------------------- server-side only
def check_no_client_classes() -> None:
    """Tamekind is server-side: no client entrypoint, no rendering, no packets.

    A net.minecraft.client reference compiles fine against the merged dev classpath and
    throws NoClassDefFoundError on a real dedicated server.
    """
    for f in java_files():
        src = f.read_text(encoding="utf-8")
        for m in re.finditer(r"\bnet\.minecraft\.client\.[\w.]+", src):
            line = src[: m.start()].count("\n") + 1
            errors.append(
                f"client class in a server-side mod: {f.relative_to(ROOT)}:{line} -> {m.group(0)}")

    if not FABRIC_JSON.exists():
        return
    try:
        meta = json.loads(FABRIC_JSON.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return
    # "*" is deliberate here, unlike CullTag's "server": the jar must be installable on a
    # server alone but must not refuse to load in a singleplayer client.
    if meta.get("environment") != "*":
        errors.append(
            f"metadata: fabric.mod.json environment is '{meta.get('environment')}', not '*'")
    if "client" in meta.get("entrypoints", {}):
        errors.append("metadata: fabric.mod.json declares a client entrypoint")


# ----------------------------------------------------------------- config integrity
def _config_template(src: str):
    start = src.index('return """')
    mid = src.index('""".formatted(', start)
    end = src.index("\n    }", mid)
    return src[start:mid], src[mid + len('""".formatted('):end]


def check_config_alignment() -> None:
    """The properties template and its .formatted() args must line up by name.

    TamekindConfig interpolates over a hundred args positionally. A mismatch throws
    MissingFormatArgumentException at runtime, not compile time, so the config silently
    stops saving.
    """
    src = CONFIG.read_text(encoding="utf-8")
    try:
        body, blob = _config_template(src)
    except ValueError:
        errors.append("config: could not locate the properties template")
        return

    keys = [
        m.group(1)
        for line in body.splitlines()
        if not line.strip().startswith("#")
        and (m := re.match(r"([A-Za-z0-9_]+)=(%[sd])\s*$", line.strip()))
    ]

    depth, args, cur = 0, [], ""
    for ch in blob:
        if ch in "([":
            depth += 1
        elif ch in ")]":
            if depth == 0:
                break
            depth -= 1
        if ch == "," and depth == 0:
            args.append(cur.strip())
            cur = ""
        else:
            cur += ch
    if cur.strip().rstrip(");").strip():
        args.append(cur.strip().rstrip(");").strip())
    args = [a for a in args if a]

    if len(keys) != len(args):
        errors.append(f"config: {len(keys)} template keys but {len(args)} format args")
    for key, arg in zip(keys, args):
        # Values are wrapped for formatting: Double.toString(x), (float) y and so on.
        inner = re.sub(r"^.*\(|\)$", "", arg).strip() or arg
        if key not in (arg, inner):
            errors.append(f"config: key '{key}' is fed by '{arg}'")


def check_config_fields_wired() -> None:
    """A knob has to be read from the file, written back to it, and actually consulted.

    A field nothing outside the config class reads looks configurable and is not.
    """
    src = CONFIG.read_text(encoding="utf-8")
    fields = [
        m.group(2)
        for m in re.finditer(r"^    public static (?:volatile )?(boolean|int|double|float|String) (\w+)\s*=", src, re.M)
    ]
    others = "\n".join(f.read_text(encoding="utf-8") for f in java_files() if f != CONFIG)
    for name in fields:
        if name == "activeProfile":
            continue  # set by applyProfile, not parsed from the file
        if f'"{name}"' not in src:
            errors.append(f"config: field '{name}' is never read from the properties file")
        if f"{name}=%" not in src:
            errors.append(f"config: field '{name}' is missing from the properties template")
        if not re.search(r"\bTamekindConfig\." + re.escape(name) + r"\b", others):
            errors.append(
                f"config: field '{name}' is never read outside TamekindConfig "
                f"(a knob wired to nothing looks configurable and is not)")


# CullTag audits that every config key appears in its README. Deliberately not copied:
# that mod has a handful of knobs, Tamekind has over a hundred, and its README points at
# the fully-commented properties file on purpose rather than duplicating it. Warband, the
# closest sibling by config size, does not carry the check either. What is checked instead
# is that every key is genuinely wired, in check_config_fields_wired.


# -------------------------------------------------------------- goal arbitration
def goal_flags() -> dict[str, set[str]]:
    """Which goal classes hold which goal-selector flags."""
    flags: dict[str, set[str]] = {}
    for f in sorted(GOALS.rglob("*.java")):
        src = f.read_text(encoding="utf-8")
        m = re.search(r"setFlags\(EnumSet\.(\w+)\(([^)]*)\)\)", src)
        if m and m.group(1) != "noneOf":
            flags[f.stem] = set(re.findall(r"Flag\.(\w+)", m.group(2)))
        else:
            flags[f.stem] = set()
    return flags


def injected_priorities() -> dict[int, list[str]]:
    """Goal name by priority, resolving the GoalPriorities constants the injector uses."""
    if not (INJECTOR.exists() and PRIORITIES.exists()):
        return {}
    consts = {
        m.group(1): int(m.group(2))
        for m in re.finditer(r"public static final int (\w+)\s*=\s*(\d+);",
                             PRIORITIES.read_text(encoding="utf-8"))
    }
    src = strip_comments_and_strings(INJECTOR.read_text(encoding="utf-8"))
    by_priority: dict[int, list[str]] = {}
    for m in re.finditer(r"addGoal\(\s*(?:GoalPriorities\.)?(\w+)\s*,\s*new (\w+)\(", src):
        token, goal = m.group(1), m.group(2)
        priority = consts.get(token, int(token) if token.isdigit() else None)
        if priority is None:
            continue
        by_priority.setdefault(priority, []).append(goal)
    return by_priority


def check_goal_priorities_centralised() -> None:
    """Priorities are claims against a table vanilla owns, so they live in one place.

    A bare integer at an addGoal call site is a priority chosen without looking at the
    table. GoalPriorities carries vanilla's layout and the reason for each slot.
    """
    if not INJECTOR.exists():
        return
    src = strip_comments_and_strings(INJECTOR.read_text(encoding="utf-8"))
    for m in re.finditer(r"addGoal\(\s*(\d+)\s*,", src):
        line = src[: m.start()].count("\n") + 1
        errors.append(
            f"raw goal priority: {INJECTOR.relative_to(ROOT)}:{line} -> addGoal({m.group(1)}, ...) "
            f"(use a GoalPriorities constant so the reasoning stays with the number)")


def check_goal_priority_ties() -> None:
    """Two goals at one priority holding the same flag cannot preempt each other.

    The selector only lets a strictly higher-priority goal take a flag, so a tie means
    whichever started first keeps control. HerdFollowGoal shipped tied against vanilla's
    random stroll and won a coin flip instead of leading the herd. Ties within Tamekind's
    own goals are what this catches; /tamekind goals catches ties against vanilla and
    other mods at runtime.
    """
    flags = goal_flags()
    for priority, names in sorted(injected_priorities().items()):
        movers = sorted({n for n in names if "MOVE" in flags.get(n, set())})
        if len(movers) > 1:
            warnings.append(
                f"goal priority {priority}: {len(movers)} MOVE-holding goals tie "
                f"({', '.join(movers)}) - equal priorities cannot preempt each other")


def check_flagless_goals_moving_mobs() -> None:
    """A flagless goal that moves the animal gets no arbitration and must yield itself."""
    flags = goal_flags()
    for f in sorted(GOALS.rglob("*.java")):
        if flags.get(f.stem):
            continue
        src = strip_comments_and_strings(f.read_text(encoding="utf-8"))
        if "setDeltaMovement" not in src and "getNavigation().moveTo" not in src:
            continue
        if "distanceToSqr" in src:
            continue  # has some explicit proximity yield
        warnings.append(
            f"flagless goal moves the animal without an obvious yield: "
            f"{f.relative_to(ROOT)} ({f.stem}) - no goal flag means no arbitration")


def check_movement_goals_respect_farms() -> None:
    """Never break vanilla farms.

    Leashed, mounted, named, breeding and tamed animals opt out of Tamekind movement via
    TamekindAnimalRules.skipMovementGoals. Anything that moves an animal must consult it,
    or a pen full of name-tagged cows starts wandering.
    """
    flags = goal_flags()
    for f in sorted(GOALS.rglob("*.java")):
        if "MOVE" not in flags.get(f.stem, set()):
            continue
        src = f.read_text(encoding="utf-8")
        if "skipMovementGoals" in src:
            continue
        errors.append(
            f"movement goal ignores farm opt-out: {f.relative_to(ROOT)} ({f.stem}) "
            f"holds MOVE but never calls TamekindAnimalRules.skipMovementGoals")


# ----------------------------------------------------------- fully-qualified names
def check_inline_qualified_names() -> None:
    """`com.tamekind.x.Y.z()` inline instead of an import. Reads as unfinished."""
    for f in java_files():
        src = strip_comments_and_strings(f.read_text(encoding="utf-8"))
        src = re.sub(r"^import .*$", "", src, flags=re.M)
        hits = re.findall(r"\bcom\.tamekind\.[\w.]+\.[A-Z]\w+", src)
        if hits:
            distinct = sorted(set(hits))
            shown = ", ".join(t.rsplit(".", 1)[-1] for t in distinct[:4])
            more = f" +{len(distinct) - 4} more" if len(distinct) > 4 else ""
            warnings.append(
                f"inline qualified names: {f.relative_to(ROOT)} "
                f"({len(hits)} refs: {shown}{more}) - import instead")


# --------------------------------------------------------------- displayed text
LITERAL_TEXT_EXEMPT = {"command"}
_PROSE = re.compile(r"[A-Za-z]{2,}\s+[A-Za-z]{2,}")
NOT_KEY_SUFFIXES = {"properties", "json", "txt", "log", "jar", "accesswidener", "toml"}


def _literal_args(src: str):
    """Yields (argument_source, line) for every Component.literal(...) call."""
    for m in re.finditer(r"Component\.literal\(", src):
        i, depth = m.end(), 1
        while i < len(src) and depth:
            if src[i] == '"':
                i += 1
                while i < len(src) and src[i] != '"':
                    i += 2 if src[i] == "\\" else 1
            elif src[i] == "(":
                depth += 1
            elif src[i] == ")":
                depth -= 1
                if not depth:
                    break
            i += 1
        yield src[m.end():i], src[: m.start()].count("\n") + 1


def check_hardcoded_display_text() -> None:
    """Component.literal("some English") shipped to a player cannot be translated.

    Tamekind is server-side, so the player is normally on a vanilla client that has never
    heard of this mod. Component.translatable alone would show them the raw key, so the
    answer is always translatableWithFallback(key, english): a vanilla client renders the
    English and a client with the language file renders the translation.
    """
    for f in java_files():
        if f.parent.name in LITERAL_TEXT_EXEMPT:
            continue
        src = f.read_text(encoding="utf-8")
        for arg, line in _literal_args(src):
            texts = re.findall(r'"((?:\\.|[^"\\])*)"', arg)
            if any(t.startswith("/") for t in texts):
                continue
            prose = [t for t in texts if _PROSE.search(t)]
            glued = "+" in arg and any(t.strip() for t in texts)
            if not prose and not glued:
                continue
            shown = (prose[0] if prose else next(t for t in texts if t.strip()))[:48]
            detail = "concatenated display text" if glued and not prose else "hardcoded display text"
            errors.append(
                f"{detail}: {f.relative_to(ROOT)}:{line} -> "
                f'"{shown}" (use Component.translatableWithFallback)')


def check_bare_translatable() -> None:
    """Component.translatable without a fallback shows a vanilla client the raw key."""
    for f in java_files():
        src = f.read_text(encoding="utf-8")
        for m in re.finditer(r"Component\.translatable\(", src):
            line = src[: m.start()].count("\n") + 1
            errors.append(
                f"translatable without fallback: {f.relative_to(ROOT)}:{line} "
                f"(server-side mod: use translatableWithFallback)")


def check_lang_keys_resolve() -> None:
    """Every tamekind.* key used in code should exist in en_us.json, and vice versa."""
    used: set[str] = set()
    prefixes: set[str] = set()
    for f in java_files():
        src = f.read_text(encoding="utf-8")
        for key in re.findall(r'"(tamekind\.[\w.]+)"', src):
            if key.rsplit(".", 1)[-1] in NOT_KEY_SUFFIXES:
                continue
            (prefixes if key.endswith(".") else used).add(key)

    if not LANG.exists():
        if used or prefixes:
            errors.append(
                f"lang: {LANG.relative_to(ROOT).as_posix()} is missing but "
                f"{len(used) + len(prefixes)} key(s) are used in code")
        return
    try:
        keys = set(json.loads(LANG.read_text(encoding="utf-8")))
    except json.JSONDecodeError as exc:
        errors.append(f"lang: en_us.json does not parse ({exc})")
        return

    for key in sorted(used - keys):
        errors.append(f"lang: '{key}' is used in code but missing from en_us.json")
    for prefix in sorted(prefixes):
        if not any(k.startswith(prefix) for k in keys):
            errors.append(f"lang: no key starts with '{prefix}'")
    for key in sorted(keys - used):
        if any(key.startswith(p) for p in prefixes):
            continue
        warnings.append(f"lang: '{key}' is defined but never used")


def check_lang_fallbacks_match() -> None:
    """The English in translatableWithFallback must match en_us.json, or the two drift."""
    if not LANG.exists():
        return
    try:
        lang = json.loads(LANG.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return
    pattern = re.compile(
        r'translatableWithFallback\(\s*"(tamekind\.[\w.]+)"\s*,\s*"((?:\\.|[^"\\])*)"', re.S)
    for f in java_files():
        for m in pattern.finditer(f.read_text(encoding="utf-8")):
            fallback = m.group(2).replace(r"\"", '"').replace(r"\n", "\n").replace(r"\\", "\\")
            key = m.group(1)
            if key in lang and lang[key] != fallback:
                errors.append(
                    f"lang: fallback for '{key}' does not match en_us.json "
                    f"({fallback!r} vs {lang[key]!r})")


# ----------------------------------------------------------- per-mod id hardcoding
def check_hardcoded_mod_ids() -> None:
    """Matching registry ids in Java only ever recognises the ids somebody typed in.

    Tags are the vanilla answer and any datapack can extend them. Tamekind is already
    tag-driven nearly everywhere, so a getPath() comparison stands out.
    """
    for f in java_files():
        src = strip_comments_and_strings(f.read_text(encoding="utf-8"))
        if "getPath()" not in src:
            continue
        compares = re.findall(r"getPath\(\)[^;\n]*?\.(?:equals|contains|startsWith)\(", src)
        compares += re.findall(r"\w+\.(?:equals|contains|startsWith)\(", src) if "getPath()" in src else []
        if not re.search(r"getPath\(\)", src):
            continue
        if re.search(r"\.(equals|contains|startsWith)\(\s*\"", src):
            warnings.append(
                f"registry path matching: {f.relative_to(ROOT)} compares getPath() against "
                f"string literals - prefer an entity_type tag so datapacks can extend it")


# ------------------------------------------------------------------- metadata
def check_declared_license() -> None:
    """fabric.mod.json's license is what a user sees in the mod list and on Modrinth."""
    if not FABRIC_JSON.exists():
        errors.append("metadata: fabric.mod.json is missing")
        return
    try:
        meta = json.loads(FABRIC_JSON.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        errors.append(f"metadata: fabric.mod.json does not parse ({exc})")
        return
    declared = meta.get("license", "")
    license_path = ROOT / "LICENSE"
    if declared and not license_path.exists():
        errors.append(f"metadata: fabric.mod.json declares '{declared}' but there is no LICENSE file")
        return
    if license_path.exists():
        text = license_path.read_text(encoding="utf-8")
        if "MIT License" in text and declared != "MIT":
            errors.append(f"metadata: LICENSE is MIT but fabric.mod.json declares '{declared}'")


def check_declared_icon() -> None:
    """The icon path in fabric.mod.json is what the mod list and Modrinth render.

    A declared path with no file behind it shows a blank tile and nothing logs it. The
    icon is generated by scripts/make_icon.py, so a stale or renamed output would break
    silently.
    """
    if not FABRIC_JSON.exists():
        return
    try:
        meta = json.loads(FABRIC_JSON.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return
    icon = meta.get("icon")
    if not icon:
        warnings.append(
            "metadata: fabric.mod.json declares no icon, so the mod list and Modrinth "
            "show a blank tile (run scripts/make_icon.py)")
        return
    path = RESOURCES / icon
    if not path.exists():
        errors.append(
            f"metadata: fabric.mod.json declares icon '{icon}' but "
            f"{path.relative_to(ROOT).as_posix()} does not exist "
            f"(run scripts/make_icon.py)")
        return
    try:
        from PIL import Image
    except ImportError:
        return  # Pillow is only needed to generate; do not require it to audit.
    with Image.open(path) as img:
        w, h = img.size
    if w != h:
        errors.append(f"metadata: icon is {w}x{h}, must be square")
    elif w not in (128, 256, 512):
        warnings.append(f"metadata: icon is {w}x{w}; 128 is the Fabric convention")


# ------------------------------------------------------------------- house style
def check_no_em_dashes() -> None:
    """No em dashes anywhere in the repo.

    A house style rule rather than a correctness one, but a mechanical one, so it is
    checked mechanically instead of being remembered. Use a comma for an aside, a colon
    before an explanation, a semicolon between two full clauses, or parentheses. Do not
    substitute blindly: a comma is wrong about a quarter of the time and gives you a
    comma splice.
    """
    tracked = [
        *java_files(),
        *sorted(ROOT.rglob("src/test/**/*.java")),
        *sorted(ROOT.glob("*.md")),
        *(sorted((ROOT / "docs").rglob("*.md")) if (ROOT / "docs").is_dir() else []),
        *(sorted((ROOT / "wiki").rglob("*.md")) if (ROOT / "wiki").is_dir() else []),
        *(sorted((ROOT / ".claude").rglob("*.md")) if (ROOT / ".claude").is_dir() else []),
        *sorted((ROOT / "scripts").glob("*.py")),
        ROOT / "build.gradle",
        ROOT / "gradle.properties",
        FABRIC_JSON,
    ]
    if LANG.exists():
        tracked.append(LANG)

    for f in tracked:
        if not f.is_file():
            continue
        try:
            src = f.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        for n, line in enumerate(src.splitlines(), 1):
            if EM_DASH in line:
                errors.append(
                    f"em dash: {f.relative_to(ROOT).as_posix()}:{n} "
                    f"(use a comma, colon, semicolon or parentheses)")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--quiet", action="store_true", help="errors only")
    args = parser.parse_args()

    for check in (
        check_unused_imports,
        check_unused_private_members,
        check_mixin_unique,
        check_mixins_registered,
        check_mixin_target_names,
        check_mixin_hardcoded_indexes,
        check_no_minecraft_reflection,
        check_no_blockpos_getcenter,
        check_singular_tag_directories,
        check_tag_files_exist,
        check_no_client_classes,
        check_config_alignment,
        check_config_fields_wired,
        check_goal_priorities_centralised,
        check_goal_priority_ties,
        check_flagless_goals_moving_mobs,
        check_movement_goals_respect_farms,
        check_inline_qualified_names,
        check_hardcoded_display_text,
        check_bare_translatable,
        check_lang_keys_resolve,
        check_lang_fallbacks_match,
        check_hardcoded_mod_ids,
        check_declared_license,
        check_declared_icon,
        check_no_em_dashes,
    ):
        check()

    if errors:
        print(f"ERRORS ({len(errors)}) - fix before shipping")
        for e in errors:
            print("  " + e)
    if warnings and not args.quiet:
        print(f"\nWARNINGS ({len(warnings)}) - review, may be intentional")
        for w in warnings:
            print("  " + w)
    if not errors and (args.quiet or not warnings):
        print("clean")
    elif not errors:
        print(f"\nno errors ({len(warnings)} warnings)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
