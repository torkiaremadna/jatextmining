#The tutorial for jatextmining in Japanese



# はじめに #

jatextminingはHadoopを用いて大規模な日本語テキストデータを高速にテキストマイニングするためのツールです．

順次実装ができしだい分析手法は追加して行きたいと思いますが，現在は以下の基本的な分析のみサポートしています．
  * 頻度分析
  * 共起分析
    * Χ二乗検定
    * 相互情報量

Linux環境が無く、まず簡単に試してみ
たいという方には，VMwareを用いてWindows上での試す方法を解説した[Tutorial\_with\_cloudera](http://code.google.com/p/jatextmining/wiki/Tutorial_with_cloudera)をで動作を試すことができます．[Tutorial\_with\_cloudera](http://code.google.com/p/jatextmining/wiki/Tutorial_with_cloudera)では，wikipediaのデータを用いた，簡単な実データを用いた使用法の解説もあります．

# 前提条件 #

  * jatextminingはLinuxのようなUnix系OS上での動作を前提としています
  * jatextminingはHadoop上での動作を前提としています．Hadoopのクラスター環境を構築のうえでご利用ください．
    * Hadoop 擬似分散環境(pseudo)でももちろん動作します．しかし遅いです．
    * Hadoopのヴァージョンは0.20.2での動作確認をしています．
  * jatextminingは形態素解析にGoSenを用いています．あらかじめGoSenをコンパイルする必要があります．
    * 将来的には形態素解析部分は抽象化したいと思います．

# 準備 #



## GoSenの準備 ##

  * 形態素解析に形態素解析器MeCabのJavaクローンであるGoSenを用います
    * GoSen配布ページ : http://itadaki.svn.sourceforge.net/viewvc/itadaki/GoSen/
  * GoSenを/usr/local/GoSenにインストールします
    * インストール先を任意のパスに変更する場合はjatextmining/conf/jatextmining.xmlのjatextmining.GoSenの値をインストール先のパスに変更してください．

```
  # GoSenのソースを入手する
  svn co  https://itadaki.svn.sourceforge.net/svnroot/itadaki/GoSen GoSen

  # コンパイルする
  cd GoSen
  ant

  # 辞書をコンパイルする
  cd testdata/dictionary/
  ant

  # 動作確認
  cd ../../
  java -cp $CLASSPATH:bin:gosen-1.0beta.jar examples.StringTaggerDemo testdata/dictionary/dictionary.xml
  Please input Japanese sentence:
  残暑が厳しい．
  残暑    (残暑)  名詞-一般(0,2,2)        ザンショ        ザンショ
  が      (が)    助詞-格助詞-一般(2,3,1) ガ      ガ
  厳しい  (厳しい)        形容詞-自立(3,6,3)      キビシイ        キビシイ
  ．      (．)    記号-句点(6,7,1)        ．   

  # /usr/localにコピーします
  cd ../
  sudo cp -R GoSen /usr/local/
```

## hadoopの環境構築 ##

  * Hadoopの本家サイトからHadoopのパッケージをダウンロードし，Hadoop環境を構築してください
    * http://hadoop.apache.org/common/
  * 以下のサイトがHadoop環境の構築に役立ちます
    * http://hadoop.apache.org/common/docs/current/
    * http://www.atmarkit.co.jp/fjava/special/distributed03/distributed03_1.html
  * Hadoopを初めて使う方は、1台のホストでHadoopを使うSigle Nodeで実行すると良いでしょう
    * Single Nodeでセットアップするには以下のドキュメントを参照してください。
    * http://hadoop.apache.org/common/docs/current/single_node_setup.html
    * 基本的にはconf/core-site.xml、conf/core-site.xml、conf/core-site.xmlの3つConfigurationを修正すれば環境を構築できます。Configurationについては以下のドキュメントを参照してください。
    * http://hadoop.apache.org/common/docs/current/single_node_setup.html#Configuration
  * hadoop-env.shの修正
    * hadoopにGoSenなどのクラスパスを追加します
    * hadoop-env.shに以下の行を追加してください(基本的には1行目に追加すればOKです)。

```
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/usr/local/GoSen/gosen-1.0beta.jar:/usr/local/GoSen/jisx0213-1.0.jar
```

## jatextminingの準備 ##

  * HadoopとGoSenを使う準備ができたらjatextmining使えます
  * 任意のディレクトリにjatextminingをダウンロードしてjarファイルを生成してください

```
  # http://code.google.com/p/jatextmining/downloads/listから最新版をダウンロードしてください

  # tarアーカイブを解凍します
  tar zxvf jatextmining-*.*.tar.gz

  # jarファイルを生成します
  cd jatextmining-*.*
  ant
```

# 使い方 #

  * わかりやすさのためにHadoopの使い方交えながら説明したいと思います
  * 頻度分析と共起分析に分けて説明したいと思います

## 頻度分析 ##

  * 複数の文書から，各単語の文書頻度をカウントします．(将来的には単なる頻度もサポートしたいとおもいます)

  * 頻度分析のコマンド解説
    * -i 入力ファイルのパスを指定します(HDFS上のパスです)
    * -o 計算結果の出力先のパスを指定します(HDFS上のパス)
    * (option) -w キーワードランキングをする際に，過去の文書のパスを指定します．(頻度の割合で重み付けをするため)
    * (option) -p 抽出する単語の頻度を指定します．デフォルトはcompNoun(複合名詞)を抽出します．
      * noun     : 名詞
      * compNoun : 複合名詞
      * adj      : 形容詞

```
$HADOOP_HOME/bin/hadoop jar jatextmining-0.1.jar wordcount

Usage:
  wordcount -i in -o out

Option:
  -w df_doc: Spefify the document path for document freaquency database.
  -p [noun|compNoun|adj]: Specify the extracting pos.

Example:
  hadoop jar jatextmining-*.* wordcount -i input -o output
  hadoop jar jatextmining-*.* wordcount -i input -o output -w df_doc -p noun
```

  * 以下に頻度分析の具体的な実行方法を説明したいと思います
  * テキストの用意とHadoopへのコピーをします
  * サンプルのテキストデータとして以下のテキストをここでは使います

  * 入力テキストフォーマット
    * jatextminingでは一行一文書のフォーマットで入力ファイルを作成してください．

```
emacs input1.txt
私は今日、電車に乗って会社へ行きました。満員電車はとても暑かったです。
私は昨日、車に乗って会社へ行きました。高速道路なので早く到着しました。
彼は今日、電車に乗って会社へ行きました。満員電車だったので暑かったそうです。
彼は昨日、車に乗って会社へ行きました。高速道路なので早く到着したそうです。
彼女は、電車に乗らず、車で会社へ行きました。首都高速道路なので早く到着したそうです。
```

  * Hadoopにデータをコピーします

```
$HADOOP_HOME/bin/hadoop fs -put input1.txt input1.txt
```

  * コピーできてるか確認します

```
$HADOOP_HOME/bin/hadoop fs -cat input1.txt
私は今日、電車に乗って会社へ行きました。満員電車はとても暑かったです。
私は昨日、車に乗って会社へ行きました。高速道路なので早く到着しました。
彼は今日、電車に乗って会社へ行きました。満員電車だったので暑かったそうです。
彼は昨日、車に乗って会社へ行きました。高速道路なので早く到着したそうです。
彼女は、電車に乗らず、車で会社へ行きました。首都高速道路なので早く到着したそうです。
```

  * 頻度分析をします
  * input1.txtから各名詞の頻度をカウントします
    * まず最初に名詞のカウントをします
    * 次に複合名詞に頻度をカウントします(単純に名詞列をくっつけただけです)
    * 次に形容詞のカウントをします

```
# input1.txtから名詞を抽出して，頻度をカウントした結果をoutput_nounに保存します
$HADOOP_HOME/bin/hadoop jar jatextmining-0.1.jar wordcount -i input1.txt -o output_noun -p noun

# 出力結果output_nounの内容を確認してみます
$HADOOP_HOME/bin/hadoop fs -cat output_noun/* | sort -n -r -k 2
会社    5.0
の      4.0
道路    3.0
到着    3.0
電車    3.0
車      3.0
高速    3.0
そう    3.0
満員    2.0
彼      2.0
私      2.0
昨日    2.0
今日    2.0
彼女    1.0
首都    1.0

```

```
# input1.txtから複合名詞を抽出して，頻度をカウントした結果をoutput_compNounに保存します
$HADOOP_HOME/bin/hadoop jar jatextmining-0.1.jar wordcount -i input1.txt -o output_compNoun -p compNoun

# 出力結果output_compNounの内容を確認してみます
$HADOOP_HOME/bin/hadoop fs -cat output_compNoun/* | sort -n -r -k 2
会社    5.0
の      4.0
到着    3.0
電車    3.0
車      3.0
そう    3.0
満員電車        2.0
彼      2.0
私      2.0
昨日    2.0
今日    2.0
高速道路        2.0
彼女    1.0
首都高速道路    1.0

# input1.txtから形容詞を抽出して，頻度をカウントした結果をoutput_adjに保存します
$HADOOP_HOME/bin/hadoop jar jatextmining-0.1.jar wordcount -i input1.txt -o output_adj -p adj

# 出力結果output_compNounの内容を確認してみます
$HADOOP_HOME/bin/hadoop fs -cat output_adj/* | sort -n -r -k 2
早い    3.0
暑い    2.0
```

  * jatextminingは頻度分析を用いて簡単なキーワードランキングを作成できます
  * ランキングの生成方法は，ランキングを生成したい日の文書中の各頻度と，過去の文書中での頻度の割合をとってランキングを生成します．
  * キーワードランキングを作成するには以下の2つの文書を入力します
    * ランキング作成したい日の文書(today)
    * 過去数日の文書(past)
  * 今回は簡単のために先ほどのinput1.txtを2つのファイルに分割して実行してみたいと思います

```
# 二つの入力文書を作成します

# ランキングを生成したい日の文書
cat today
彼は昨日、車に乗って会社へ行きました。高速道路なので早く到着したそうです。

# 過去数日分の文書
cat past 
私は今日、電車に乗って会社へ行きました。満員電車はとても暑かったです。
私は昨日、車に乗って会社へ行きました。高速道路なので早く到着しました。
彼は今日、電車に乗って会社へ行きました。満員電車だったので暑かったそうです。
彼女は、電車に乗らず、車で会社へ行きました。首都高速道路なので早く到着したそうです。

# 文書をHadoopにコピーします
$HADOOP_HOME/bin/hadoop fs -put today today
$HADOOP_HOME/bin/hadoop fs -put past past

# pastを用いてtodayからキーワードランキングを生成します
$HADOOP_HOME/bin/hadoop jar jatextmining-0.1.jar wordcount -i today -o ranking -w past -p noun

# 結果を確認します
$HADOOP_HOME/bin/hadoop fs -cat ranking/* | sort -n -r -k 2
彼      1.0
昨日    1.0
```

上の例では，文書数が少なすぎるのであまりおもしろくありませんが，文書数が多くなればそれなりの結果になります．

## 共起分析 ##

  * jatextminingは日本語のテキストに対して共起分析ができます
  * 共起分析とか簡単に言うと，１文書中に，単語のペアが出現した頻度をカウントしたもので，単語と単語の関連性などを分析する際に用います
  * 分析対象のデータは頻度分析で用いたinput1.txtを使いたいと思います

  * 共起分析のコマンド解説
    * -i 入力ファイルのパスを指定します(HDFS上のパスです)
    * -o 計算結果の出力先のパスを指定します(HDFS上のパス)
    * (option) -p 抽出する単語の頻度を指定します．デフォルトはcompNoun(複合名詞)を抽出します．
      * noun     : 名詞
      * compNoun : 複合名詞
      * adj      : 形容詞
    * (option) -t 重み付けの手法を指定します．デフォルトはΧ二乗を持ちます．
      * freaq : 共起文書頻度
      * chi   : Χ二乗検定
      * mi    : 相互情報量
    * (option) -r ２回目以降の分析で，分析対象の文書が同一で単に重み付けを変更して再計算をしたい場合は，中間ファイルを再利用して効率的解析します

```
$HADOOP_HOME/bin/hadoop jar jatextmining-0.1.jar cooccurrence

Usage:
  cooccurrence -i in -o out -t [chi|mi|freaq]

Option:
  -t [chi|mi|freaq]: Spefify the method for weighting co-occurrence.
  -p [noun|compNoun|adj]: Sepecify the extracting words. The default is compNoun
  -r: if given this option, reuse intermediate file.

Example:
  hadoop jar jatextmining-*.* cooccurrence -i input -o output -t chi
  hadoop jar jatextmining-*.* cooccurrence -i input -o output -t chi -p noun
```

  * 共起頻度(文書頻度)をカウントする
  * input1.txtから１文書中で各単語が共起した回数をカウントします

```
# 名詞の共起頻度をカウントします(文書頻度)
$HADOOP_HOME/bin/hadoop jar jatextmining-0.1.jar cooccurrence -i input1.txt -o co_freaq -t freaq -p noun

# 結果を出力します(一部省略しています)
# フォーマットは 単語\t共起単語1\t共起頻度\t共起単語1\t共起頻度\t ...\n
$HADOOP_HOME/bin/hadoop fs -cat co_freaq/*
そう    会社3.0000      道路2.0000      高速2.0000      到着2.0000      電車2.0000      彼女1.0000
今日    会社2.0000      満員2.0000      電車2.0000      そう1.0000
会社    到着3.0000      道路3.0000      高速3.0000      そう3.0000      電車3.0000      昨日2.0000
到着    高速3.0000      道路3.0000      会社3.0000      そう2.0000      昨日2.0000      首都1.0000
彼女    会社1.0000      到着1.0000      道路1.0000      電車1.0000      首都1.0000      高速1.0000
昨日    会社2.0000      到着2.0000      道路2.0000      高速2.0000      そう1.0000
満員    今日2.0000      電車2.0000      会社2.0000      そう1.0000
道路    到着3.0000      高速3.0000      会社3.0000      昨日2.0000      そう2.0000      首都1.0000
電車    会社3.0000      今日2.0000      満員2.0000      そう2.0000      彼女1.0000      首都1.0000
首都    会社1.0000      到着1.0000      彼女1.0000      道路1.0000      電車1.0000      高速1.0000
高速    道路3.0000      到着3.0000      会社3.0000      昨日2.0000      そう2.0000      電車1.0000
```

  * 頻度ではなく重み付けをΧ二乗検定を用いることができます
    * Χ二乗検定に詳いては詳しくこちらを参照ください http://ja.wikipedia.org/wiki/%E3%82%AB%E3%82%A4%E4%BA%8C%E4%B9%97%E6%A4%9C%E5%AE%9A
      * 通常，単純に共起頻度を計算するよりも，Χ二乗検定や，相互情報量などの重み付け修正をしたがほうより，関連性の高い共起単語が上位になります

```
# 重み付けにΧ二乗検定を用いて共起単語を抽出する
$HADOOP_HOME/bin/hadoop jar jatextmining-0.1.jar cooccurrence -i input1.txt -o co_chi -t chi -p noun

# 結果を出力します(一部省略しています)
$HADOOP_HOME/bin/hadoop fs -cat co_chi/*
そう    彼女0.8333      首都0.8333      今日0.1389      満員0.1389      電車0.1389      道路0.1389
今日    満員5.0000      電車2.2222      そう0.1389      会社0.0000
会社    そう0.0000      今日0.0000      到着0.0000      彼女0.0000      昨日0.0000      道路0.0000
到着    道路5.0000      高速5.0000      電車2.2222      昨日2.2222      彼女0.8333      首都0.8333
彼女    首都5.0000      到着0.8333      道路0.8333      電車0.8333      そう0.8333      高速0.8333
昨日    高速2.2222      到着2.2222      道路2.2222      そう0.1389      会社0.0000
満員    今日5.0000      電車2.2222      そう0.1389      会社0.0000
道路    高速5.0000      到着5.0000      電車2.2222      昨日2.2222      首都0.8333      彼女0.8333
電車    道路2.2222      満員2.2222      高速2.2222      到着2.2222      今日2.2222      彼女0.8333
首都    彼女5.0000      到着0.8333      そう0.8333      道路0.8333      電車0.8333      高速0.8333
高速    道路5.0000      到着5.0000      電車2.2222      昨日2.2222      彼女0.8333      首都0.8333
```

  * 重み付けに相互情報量を用いる
    * 相互情報量について詳しくはこちら後参照ください http://ja.wikipedia.org/wiki/%E7%9B%B8%E4%BA%92%E6%83%85%E5%A0%B1%E9%87%8F

```
# 相互情報量を用いた共起分析
$HADOOP_HOME/bin/hadoop jar jatextmining-0.1.jar cooccurrence -i input1.txt -o co_mi -t mi -p noun

# 相互情報量を用いた共起分析の結果
$HADOOP_HOME/bin/hadoop fs -cat co_mi/*
そう    彼女2.7081      首都2.7081      道路2.3026      高速2.3026      電車2.3026      到着2.3026
今日    満員2.3026      電車1.8971      会社1.3863      そう1.2040
会社    そう3.2189      今日3.2189      到着3.2189      彼女3.2189      昨日3.2189      道路3.2189
到着    首都2.7081      高速2.7081      昨日2.7081      道路2.7081      彼女2.7081      そう2.3026
彼女    首都1.6094      到着0.5108      道路0.5108      電車0.5108      そう0.5108      高速0.5108
昨日    高速1.8971      到着1.8971      道路1.8971      会社1.3863      そう1.2040
満員    今日2.3026      電車1.8971      会社1.3863      そう1.2040
道路    首都2.7081      高速2.7081      彼女2.7081      昨日2.7081      到着2.7081      そう2.3026
電車    彼女2.7081      今日2.7081      満員2.7081      首都2.7081      そう2.3026      会社2.1972
首都    彼女1.6094      到着0.5108      そう0.5108      道路0.5108      電車0.5108      高速0.5108
高速    到着2.7081      首都2.7081      彼女2.7081      昨日2.7081      道路2.7081      そう2.3026
```