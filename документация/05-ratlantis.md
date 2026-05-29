# Ратлантис (Ratlantis)

## Замысел автора

С **1.19+** Ратлантис — не отдельный мод-аддон, а **встроенный набор данных** (datapack), который игрок/сервер **включает по желанию**.  
По легенде Modrinth: далёкая враждебная земля с легендарными существами; попадают через редкий токен от крысы; оттуда «экстремальные» улучшения.

По умолчанию **выключен** (`ratlantisEnabledByDefault = false`).

## Включение в игре

1. При создании мира — выбор набора данных «Ratlantis» (регистрируется в `addRatlantisDatapack`).
2. В работающем мире — пакет `ChangeRatlantisStatusPacket` переключает статус.
3. Флаг `RatsMod.RATLANTIS_DATAPACK_ENABLED` отслеживает состояние.

Путь к данным: `src/main/resources/data/minecraft/datapacks/ratlantis/`.

## Измерение

| Компонент | Класс |
|-----------|--------|
| Регистрация измерения | `RatlantisDimensionRegistry` |
| Телепорт | `RatlantisTeleporter` |
| Портал | `ratlantis_portal` + `RatlantisPortalBlockEntity` |
| Токен | `chunky_cheese_token` — создаёт портал |

Редкость предметов: `RatsMod.RATLANTIS_SPECIAL` (зелёный цвет редкости).

## Генерация мира

| Регистр | Содержимое |
|---------|------------|
| `RatlantisBiomeRegistry` | Биомы |
| `RatlantisConfiguredFeatureRegistry` | Настроенные черты |
| `RatlantisPlacedFeatureRegistry` | Размещение |
| `RatlantisStructureRegistry` | Структуры |
| `RatlantisFeatureRegistry` | Карверы, руины, деревья pirat |

### Порт 1.21.1 (2026-05-22)

- `RatlantisDimensionRegistry`: эффекты неба через `ResourceLocation.withDefaultNamespace`; шум островов — публичные ключи плотностей (`shift_x`, `shift_z`, `overworld_amplified/sloped_cheese`).
- `RatlantisStructureRegistry`: в процессоре структур `GRASS_BLOCK` вместо удалённого `GRASS`.
- `RatlantisFeatureRegistry` / `CopyInputStateRuleProcessor` / `ThickBranchingTrunkPlacer`: регистрация через `MapCodec` (требование 1.21.1).

Примеры классов мира:
- `RatlantisRuinFeature` — руины
- `RatsCaveCarver` — пещеры
- `PiratTreeGrower` — деревья pirat
- `MarblePileFeature` — кучи мраморного сыра

## Блоки и ресурсы Ратлантис

Отдельный реестр `RatlantisBlockRegistry`:

- **Marbled cheese** (сырьё, кирпич, плитка, ступени, плиты) — строительный камень измерения
- **Pirat wood** — порода дерева «pirat» (кора, доски, двери, заборы)
- **Oratchalcum** — руда и слитки (огнестойкие)
- **Ratglove** — цветок и лепестки
- **Ratlantis reactor** — реактор
- **Dutchrat bell** — колокол босса
- **Automaton head** — голова для сборки автоматона

## Сущности (27 типов)

### Боссы и элита

| Сущность | Описание |
|----------|----------|
| `RatlanteanAutomaton` | Голем из мраморного сыра, луч/ближний бой |
| `NeoRatlantean` | Нео-ратлантиец, лазеры, порталы, метание блоков |
| `Dutchrat` | Летучий Голландец-крыса, мечи |
| `RatBaron` + `RatBaronPlane` | Барон на самолёте |
| `RatProtector` | Страж |

### Обычные и декор

| Сущность | Описание |
|----------|----------|
| `FeralRatlantean` | Дикие выжившие |
| `RatlanteanSpirit` | Призраки |
| `RatlanteanRatbot` | Роботы-крысы |
| `Pirat`, `GhostPirat` | Пираты |
| `Ratfish` | Рыба-крыса |
| `RattlingGun` | Пушка на колёсах |
| `PiratBoat`, лодки из pirat wood | Транспорт |

### Ездовые для крыс

- `RatAutomatonMount`
- `RatBiplaneMount` (`Plane` интерфейс)

## Улучшения линейки

См. [03-улучшения.md](03-улучшения.md) — 10 предметов `rat_upgrade_*` в `RatlantisItemRegistry`.

**Nonbeliever** — «одно улучшение, чтобы править всеми» (статы из конфига: 350 HP, 40 урона, 20 брони).

**Archeologist** — прогрессия без похода в измерение (археологические рецепты).

## Достижения

Отдельная ветка `advancement.ratlantis.*` в `en_us.json` — юмористические отсылки (Rat Baron, Dutchrat, Neo-Ratlantean и т.д.).

## Отдельный пакет данных

`RatsMod` кэширует список `RATLANTIS_ITEMS` для проверок «предмет из Ратлантис» (например, ограничения крафта/дропа).

## Связь с основным модом

- Мраморный сыр (`marbled_cheese_raw`) есть и в основном реестре блоков.
- Баннеры: `rat_and_sickle`, rat, cheese (теги `RatsBannerPatternRegistry`).
- Вставка в ванильные механики: цветочный горшок для ratglove и pirat sapling.
