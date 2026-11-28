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
- New top level `max_fall_distance` caps the drop a villager may plan, `3` is vanilla and lower values refuse deeper drops, a file without the key keeps the vanilla limit
- The generated config no longer repeats values that vanilla already produces: `fire`, `soul_fire`, `magma_block`, `sweet_berry_bush` and `cactus` left `walk_on`, `lava`, `fire`, `soul_fire`, `sweet_berry_bush`, `cactus`, `powder_snow` and `default` left `walk_through`
- `minecraft:air` left `walk_through`, so a node on a listed floor falls back to `max(total, vanilla)` again and vanilla `DANGER_FIRE` 16, `DANGER_OTHER` 8, `WATER_BORDER` 8 and `STICKY_HONEY` 8 are no longer suppressed
- 14 profession entries that repeated the global value were removed, the global cell now answers for them with the same number
- Cheaper path cost lookup: profiles are resolved by `VillagerProfession` identity instead of a profession id string, the jump penalty is precomputed per profession, and the avoid scan uses a per snapshot block index, a reusable flag array, a pooled position and a precomputed radius
- Tag ties now resolve by safety instead of tag id alone: a negative value first, then the larger value, then the tag id, and a conflicting pair is logged once per rebuild and counted in `conflicts=`
- A tag hit can no longer lift a vanilla negative malus, only an exact block id can, so a broad tag stops opening fences, walls, closed iron doors, leaves and lava
- `0.0` is now transparent: it adds no cost but no longer counts as matched, so a free floor still receives the vanilla context values such as `WATER_BORDER` 8 or `DANGER_FIRE` 16, and `0.0` can no longer lift a vanilla negative
- `#c:grass` is shipped in the mod data and written next to every `minecraft:short_grass` entry, modded grass blocks can join it with their own datapack tag
- New top level `ban_work_stroll`, default `true`, switches off the job site stroll so a working villager stays at its station instead of walking up to 40 blocks away, the meeting point stroll is untouched
- New top level `idle_walk_target_interval`, default `1200` ticks, is the minimum time between two idle strolls per villager, `0` disables it, babies and the panic, pre raid and raid packages keep the vanilla pacing
- New top level `idle_interval_covers_look_target`, default `false`, puts the look target behaviour behind the same idle interval

## 1.0.0
- Initial release