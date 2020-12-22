![Kotlin CI with Gradle](https://github.com/T45K/CLIONE/workflows/Kotlin%20CI%20with%20Gradle/badge.svg)

<p>
<img src="./logo.png" alt="logo" width=5% height=5% align=middle>
< Hello.
</p>

[[EN](#What's-CLIONE)] [[JP](#CLIONEとは)]

# What's CLIONE
CLIONE is a code clone modification support bot.<br>
CLIONE execution is triggered by Pull Requests, and it notifies inconsistently modified or newly added clone sets between the pull requests.

## Use as a GitHub bot
1. Click [here](https://github.com/apps/clione-bot) to register your GitHub account and repository to which you want to apply CLIONE.
2. Create `.clione/config.toml` in your project.
3. Edit `config.toml` by refering [Settings](#Settings)
4. Configuration is over. Let's create a pull request.

## Settings
`simirality` is integer type, and others are string type.
Simple example is [here](./.clione/config.toml).

|Name|Description|Default value|
|:--:|:--:|:--:|
|src|Relative path of source directory.<br>ex: `src/main/java`|`src`|
|lang|Programming language. Following languages are selectable.<br>`java`, `kotlin`,`python`, `cpp`|`java`|
|clone_detector|Clone Detector. Following detectors are selectable. Parentheses are the supported languages.<br>`NiCad(java, python, cpp)`,`SourcererCC(java, kotlin, python, cpp)`|`NiCad`|
|granularity|Granularity of clones. Followings are selectable.<br>`method`,`block`|`block`|
|similarity|Similarity of clones. If you enter an integer d between 0 and 10, the clones are detected with a similarity of 10*d%.|`8`|

## Use as a server
You can build your own CLIONE server.
1. Setting your GitHub Apps (see https://developer.github.com/apps/).
1. Download Txl and NiCad clone detector from [here](https://www.txl.ca/) and install them (if you use them).
1. `git clone git@github.com:T45K/CLIONE`
1. Fill in `src/main/resources/github.properties`, `src/main/resources/verify.properties` and `src/main/resources/resource.properties`.
1. `./gradlew run` (default port is `3000`).

## Use as a stand alone tool
You can also use CLIONE as a stand alone tool to detect modification-target clones from past pull requests.
1. Download `TXL` and `NiCad` clone detector from [here](https://www.txl.ca/) and install them (if you use them).
1. `git clone git@github.com:T45K/CLIONE`
1. Fill in `src/main/resources/resource.properties` and `src/main/resources/stand_alone.properties`.<br>Settings(src, lang, etc.,) should be described in `stand_alone.properties`.
1. `./gradlre stand_alone -Pargs = "user(or organization)_name/repo_name"`

___

# CLIONEとは

CLIONEはコードクローン修正支援ボットです．<br>
プルリクエスト作成時に，プルリクエスト内で行われた変更の前後で，一貫修正されていないクローンセットや，新しく追加されたクローンセットを通知します．

## ボットとして使う
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
|lang|プログラミング言語．以下を選択できます．<br>`java`, `kotlin`, `python`, `cpp`|`java`|
|clone_detector|クローン検出器．以下を選択できます．カッコ内は対応言語です．<br>`NiCad(java, python, cpp)`,`SourcererCC(java, kotlin, python, cpp)`|`NiCad`|
|granularity|検出するクローンの粒度．以下を選択できます．<br>`method`,`block`|`block`|
|similarity|検出するクローンの類似度．0~10までの整数値dを入力すると，10*d%の類似度でクローンを検出します．|`8`|

## サーバとして使う
自身のサーバを立ててCLIONEを使えます
1. https://developer.github.com/apps/ を参考にして，GitHub Appsを作成してください
1. NiCadを使いたい場合，[ここ](https://www.txl.ca/)からTxlとNiCadをインストールしてください
1. このリポジトリをクローンして，`src/main/resources/github.properties`，`src/main/resources/verify.properties`，`src/main/resources/resource.properties`に必要な情報を記載してください
1. `./gradlew run` （デフォルトポートは3000です）

## 単体ツールとして使う
CLIONEは単体ツールとしても利用できます．
過去のプルリクから対象のクローンを検出します．
1. NiCadを使いたい場合，[ここ](https://www.txl.ca/)からTxlとNiCadをインストールしてください
1. このリポジトリをクローンして，`src/main/resources/stand_alone.properties`，`src/main/resources/resource.properties`に必要な情報を記載してください．<br>また，`stand_alone.properties`にsrcやlangなどの設定を記載してください
1. `./gradlew stand_alone -Pargs = "user(or organization)_name/repo_name"`

# Cite
```
@inproceedings{clione,
         title = {CLIONE: Clone Modification Support for Pull Request Based Development},
        author = {Tasuku Nakagawa and Yoshiki Higo and Shinji Kusumoto},
     booktitle = {the 27th Asia-Pacific Software Engineering Conference (APSEC)},
         pages = {455-459},
         month = {12},
          year = {2020},
}
```
The paper is published [here](https://sdl.ist.osaka-u.ac.jp/pman/pman3.cgi?D=675).
