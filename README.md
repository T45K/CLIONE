![Kotlin CI with Gradle](https://github.com/T45K/CLIONE/workflows/Kotlin%20CI%20with%20Gradle/badge.svg)

<p>
<img src="./logo.png" alt="logo" width=5% height=5% align=middle>
< Hello.
</p>

CLIONEはコードクローン保守支援ボットです．<br>
プルリクエスト作成をトリガーとして実行され，プルリクエスト内で行われた変更の前後で，一貫修正されていないクローンセットや，新しく追加されたクローンセットを通知します．

## 使い方
1. [こちら](https://github.com/apps/clione-bot) からGitHubアカウントと適用したいリポジトリを登録してください．
2. CLIONEを利用したいプロジェクトに内に`.clione/config.toml`ファイルを作成してください．
3. [設定](#設定)を参考に`config.toml`を編集してください．
4. 設定は以上です．あとはプルリクエストを作成してみてください．

## 設定
`similarity`は整数型，それ以外は文字列型で記述してください．<br>
tomlの例は[こちら](./.clione/config.toml)

|名前|説明|デフォルト値|
|:--:|:--:|:--:|
|src|ソースファイルを含むディレクトリへの相対パス<br>例: `src/main/java`|`src`|
|lang|プログラミング言語．以下を選択できます．<br>`java`, `kotlin`|`java`|
|clone_detector|クローン検出器．以下を選択できます．カッコ内は対応言語です．<br>`nicad(java)`,`sourcerercc(java,kotlin)`|`nicad`|
|granularity|検出するクローンの粒度．以下を選択できます．<br>`method`,`block`|`block`|
|similarity|検出するクローンの類似度．0~10までの整数値dを入力すると，10*d%の類似度でクローンを検出します．|`8`|
