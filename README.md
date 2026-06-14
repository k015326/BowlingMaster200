# BowlingMaster200

## テスト実行（Windows / PowerShell）

プロジェクト直下のスクリプトから実行する。いずれも `cd $PSScriptRoot` でプロジェクトルートに移動してから `.\gradlew` を呼ぶ。

| スクリプト | 対象 |
|---|---|
| `.\test-ocr.ps1` | OCR パッケージ（`com.example.bowlingmaster200.ocr.*`） |
| `.\test-domain.ps1` | domain パッケージ（calculator / validator / statistics 含む） |
| `.\test-all.ps1` | 全ユニットテスト |

### OCR テスト

```powershell
.\test-ocr.ps1
```

テストクラスは `app/src/test/java/com/example/bowlingmaster200/ocr/` 配下に配置する。

### domain / calculator テスト

```powershell
.\test-domain.ps1
```

### 全テスト

```powershell
.\test-all.ps1
```

### 方針

- 実行環境は **Windows PowerShell** を前提とする
- Gradle Wrapper（`.\gradlew`）のみ使用する
- パッケージ単位で `--tests` フィルタを指定し、対象を限定して実行する
