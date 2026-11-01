# Tudur's Guns — Forge 1.20.1 版 開発メモ

このプロジェクトは、Fabric 1.21.11 版 ([TudursGuns](https://github.com/Tuduraw/TudursGuns)、Mojang マッピング) から
**ソースを自動生成**して Forge 1.20.1 でビルドします。前提MODの Forge 版 ([tudursvehiclemod-forge](https://github.com/Tuduraw/tudursvehiclemod-forge))
と同じ仕組み・同じツールで、1.21 の API は前提MODの互換レイヤー (`com.example.tudursvehiclemod.compat`) を使います。

```
src/generated/   tools/port/port.py が生成するコード・リソース (1.20.1 名に変換済み)。直接編集しない。
                 ※ 編集した場合は必ず `port.py makepatches` で tools/port/patches に保存する
src/forge/       Forge 専用コード: エントリポイント・イベント橋渡し・このMOD用の互換クラス・1.20.1 版のクラスと mixin
tools/port/      生成ツール一式 (port.py / build_cross.py / rules.json / patches / exclude.txt / autofix.py)
libs/            前提MOD (Forge 版) の jar を置く (git 管理外)
```

## 必要なもの

- JDK 17 (ForgeGradle 6 のビルド用) と JDK 21 (生成時に Fabric 版の Loom を動かす用)
- Fabric 版 (TudursGuns) を一度ビルド済みであること (Loom キャッシュに 1.21.11 の Mojang マッピングが必要)
- 前提MOD Forge 版の jar (`libs/tudursvehiclemod-1.1.1-forge-1.20.1.jar`)

## 生成とビルド

```sh
# 1. Fabric 版から src/generated を作り直す (Fabric 版を更新したときだけ)
LOOM_JAVA_HOME=/path/to/jdk21 python3 tools/port/port.py generate --mojmap ../TudursGuns

# 2. Forge 版をビルド
JAVA_HOME=/path/to/jdk17 ./gradlew build     # -> build/libs/tudursguns-0.1.0-forge-1.20.1.jar
```

`port.py refresh` は Loom を動かさずに、前回の変換結果へ rules と patches だけを掛け直します。
`sample_addon/`・`docs/` は生成の対象外です。

### 生成の流れ (前提MODと同じ)

1. **名前の変換**: 1.21.11 の intermediary 名をキーに 1.20.1 の Mojang 名へ写すマッピングを作り (`build_cross.py`)、
   Loom の `migrateClientMappings` で全ソースを書き換え
2. **rules.json**: 1.20.1 に無い API の付け替えと、呼び出し形の違いの正規表現による書き換え。
   先頭が このMOD 用 (`_why` が `TudursGuns:` のものなど)、後ろが前提MODの規則の写し
3. **exclude.txt**: 1.20.1 では作り直したクラス (下記) を生成から外す
4. **patches**: 規則にしにくい個別の修正 (`tools/port/patches/**.patch`)
5. **リソース**: データフォルダ名とレシピの JSON 形式を変換。レシピの結果に付いた このMOD のコンポーネント
   (`tudursguns:weapon` など) は、Forge のレシピの `nbt` に同じキーで入れる (下記「アイテムのデータ」)

### 編集の手順

```sh
# src/generated を直したら必ず
python3 tools/port/port.py makepatches
```

`makepatches` をせずに `generate`/`refresh` すると、src/generated の手修正は消えます。

## 1.21.11 版との違いと対応

### アイテムのデータ・挙動

| 1.21.11 | Forge 1.20.1 |
|---|---|
| データコンポーネント (`tudursguns:weapon` 等) | 前提MODの互換 `DataComponentType`。スタックの NBT に `"tudursguns:weapon"` などのキーで保存 |
| `EQUIPPABLE`・`ATTRIBUTE_MODIFIERS`・`MAX_DAMAGE` を毎 tick 設定 | Forge のアイテムのフック (`getEquipmentSlot`・`canEquip`・`getAttributeModifiers`・`getMaxDamage`・`isDamageable`・`getMaxStackSize`) で定義から返す |
| 防具の右クリック装備 | `ArmorItem.use` で空きスロットと入れ替え |
| `AttributeModifier` の Identifier | 1.20.1 は UUID。`compat/Attrs.uuid(id)` で Identifier から決まった UUID を作る |
| 兵士の自然スポーン (Fabric の BiomeModifications) | Forge のバイオーム改変 `tudursguns:enemy_soldier_spawns` (`forge/EnemySoldierSpawns`、設定は Fabric 版と同じ config) |
| スポーンの配置条件 | `SpawnPlacementRegisterEvent` (`compat/Spawns`) |
| スポーンエッグ | `ForgeSpawnEggItem` |

### クライアント

| 1.21.11 | Forge 1.20.1 |
|---|---|
| アイテム定義 (`assets/<ns>/items/*.json`) の special モデル (`tudursguns:obj_handheld` 等) | `forge/client/SpecialItemModels` が同じ JSON を読み、`ItemRendererSpecialMixin` が描画。`base` モデルの表示変換を使い、毎フレーム描く。`has_component` の条件にも対応 |
| `SpecialModelRenderer` | `compat/client/SpecialModelRenderer` (同じ形の代替。生成コードは import だけ付け替え) |
| エンティティの描画ステート (`AimRenderState` のデータキー) | 1.20.1 版の `AimRenderState`・`LivingEntityRendererAimMixin`・`BipedEntityModelAimMixin` (エンティティから直接判定) |
| OBJ 防具の描画レイヤー (Fabric の登録コールバック) | `EntityRenderersEvent.AddLayers` で人型モデルの全レンダラーに追加 (`forge/client/TudursGunsForgeClient`) |
| 空の装備アセット (`tudursguns:none`) で頭のアイテム表示を消す | `CustomHeadLayerMixin` でこのMODの防具を頭に描かない |
| HUD の `attachElementBefore` / `replaceElement` | `compat/client/HudElementRegistry` (GUI オーバーレイと `RenderGuiOverlayEvent.Pre`) |
| 地雷探知機の枠 (Gizmos.cuboid、壁越し) | `compat/client/Outlines` |
| マウスイベント (`MouseButtonEvent`)、キーのカテゴリ | rules.json で 1.20.1 の形へ |

1.20.1 版で作り直したクラス (`exclude.txt`、同名で `src/forge/java` にある):
`AimRenderState`、`ObjArmorFeatureRenderer`、`SoldierRenderer`、mixin の `BipedEntityModelAimMixin`・`LivingEntityRendererAimMixin`・
`HeldItemRendererInvoker`・`FirstPersonBaseMixin`・`GameRendererFovMixin`。`SpecialItemModelAnimatedMixin` は不要 (1.20.1 は毎フレーム描く)。

### できないこと

- 防具の 2D の見た目 (定義の `equipment_asset`): 1.20.1 には装備アセットが無いため使われません。OBJ モデルの防具は表示されます
- 1.21.11 Fabric 用のアドオン mod (jar) は動きません。アドオンパック (データ・アセット) はそのまま読み込めます

## 確認したこと (2026-11-01)

環境: Forge 1.20.1-47.4.28、前提MOD Forge 版 1.1.1、生成元 TudursGuns `6995b72` (Mojang マッピングへの移行後)。

- 専用サーバー: 起動 (例外なし)。定義の読み込み (銃 5・アタッチメント 6・投擲物 7・地雷 4・防具 6・装備 8・銃のレシピ 34)、
  `/reload`、敵兵士の召喚、防具アイテムの NBT (`tudursguns:armor`) の保存
- クライアント (開発環境、Xvfb のソフトウェア描画):
  - 銃の一人称表示 (OBJ)・弾数の HUD・構え (一人称の照準、三人称で銃を構えるポーズ)
  - OBJ 防具: プレイヤー (ヘルメット・プレートキャリア) と防具立て (重装甲・暗視ヘルメット) で部位ごとに表示、頭にアイテムとしては描かれない
  - 敵兵士: 自分のスキン、武器を持って出現 (召喚時に装備)、プレイヤーを攻撃する
  - 画面: 銃の作業台・兵士詰所 (ブロックの右クリック)
  - インベントリのアイコン (銃・投擲物・防具・ブロック)
- 未確認: 射撃・投擲・地雷・兵士詰所の出撃と経路、スコープの画面、アドオンパック (sample_addon) の読み込み
