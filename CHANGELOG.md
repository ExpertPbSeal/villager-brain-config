## Unreleased
- Tag entries such as `#minecraft:planks` and `#c:villager_job_sites` now apply, a tag that matches no block is logged once and ignored
- Negative tag values are honoured, `-1` always means blocked and no longer doubles as a missing value
- `default` inside a profession table is ignored with a single log line, generated configs no longer contain it, use the top level `default` instead
- Top level `avoids` now merge with the profession list, a profession entry wins when both name the same block
- The penalty table is built after tags are bound and rebuilt on datapack reload, so new tags work without restarting the process
- Blocks without a match fall back to `max(default, vanilla malus)`, fences, walls and closed iron doors keep their blocking malus
- Writing a positive value in both tables lifts a vanilla negative malus, one table is not enough
- The per tick cache stores the path type independent parts, so a leaf or fence node can no longer inherit a positive reading from another path type at the same position
- Generated configs cost 32.0 for standing on `#minecraft:leaves` in `walk_on`, passing through a leaf block stays blocked
- The generated config no longer contains the unknown `minecraft:altar` key
- An empty `professions` object no longer disables the top level penalties
- The per tick malus cache is keyed by position and profession, mixed professions at one node no longer share a reading
- `villager_brain_config clear-cache` clears tick caches only and keeps the built snapshot
- New `villager_brain_config dump <block> [profession] [path_type]` prints both layers, merged avoids and the final merged malus
- New top level `max_fall_distance` caps the drop a villager may plan, `3` is vanilla, the generated config uses `1` so villagers no longer walk off two block ledges, a file without the key keeps the vanilla limit

## 1.0.0
- Initial release