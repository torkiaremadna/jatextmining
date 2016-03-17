#The tutorial for jatextmining with Cloudera’s Distribution For Hadoop



# はじめに #

jatextminingを使うにはHadoopの環境を整えなければなりません．しかし，複数のサーバを用意してHadoopの環境を構築するのは容易なことではありません．そこで，WindowsのPCを用い，jatextminingを簡単に試すためにCloudera社が提供するHadoopが使える環境が整えてあるVMWareイメージを使った方法を説明したいと思います．

分析の対象はwikipediaを用いて解説したいと思います．

なお，VMWareを使った仮想環境では処理能力が著しく低下するため，もし本当に分析目的で使用する場合は，ご自身でHadoopの環境を構築されることをおすすめします．

以下の手順で説明したいと思います．
  1. Cloudera’s Distribution For Hadoop(CDH)の用意
  1. wikipediaデータの取得
  1. jatextminingを用いたwikipediaの簡単な分析

# 注意 #

ここで解説する内容は，VMWare(ヴァーチャルマシン)上での動作となるため個々の処理に非常に時間がかかります．もし，Linux環境を所持している方は，単にLinuxにHadoopをダウンロードすればここの同等の内容を試せるので，VMwareは用いずにLinux上で試されることを強くお勧めします．

このページでVMwareのヴァーチャルマシンを用いているのは，あくまでWindows環境しか用意できない方に，簡単にお試ししていただくためにCDHを使った例をあげさせてもらいました．

# Cloudera’s Distribution For Hadoop(CDH)の用意 #

CDHの環境を作る流れを解説したいと思います．CDHを使うにはVMware Playerをまずインストールし，その後VMWare Player上でCDHを走らせます．以下順に流れを解説します．

## VMware Playerのインストール ##
  * VMware社のページからから，VMare playerのインストーラーをダウンロードし，インストールします．
  * VMwareのインストーラーのダウンロードページはこちら : Cloudera’s Distribution For Hadoop
  * ダウンロードにはユーザ登録が必要となります
  * 2010年9月14日現在，VMware playerは無料で配布されています．もし有料との旨が表示されたら違ったものをダウンロードしようしている可能性があるのでお気をつけください．
  * 以降手順にしたがってVMware playerのインストールをしてください

## CDH2の取得 ##
  * 繰り返しとなりますが，これはCloudera社が提供している，Hadoopを使える環境が整ったVMware用のUbutuのヴァーチャルマシンです
  * 2010年9月14日現在では，ベータ版のCDH3がリリースされていますが，使うにはやや難があるめ，ここではCDH2(Stable release)を使います
  * CDHのダウンロードページにアクセスします : http://www.cloudera.com/downloads/
  * CDH2の「Virtual Machine」と表示されているリンクからCDH2のtarアーカイブがダウンロードできます
    * 2010年9月14日現在では，ダウンロード先のURLは次のようになっています : http://cloudera-vm.s3.amazonaws.com/cloudera-training-0.3.3.tar.bz2
    * CDH2は約1.2GBあるのでダウンロードに時間がかかります
[![](http://broomie.net/img/cdh2.png)](http://www.cloudera.com/downloads/)
  * ダウンロードが完了したらcloudera-training-0.3.3.tar.gz2を解凍します
    * windowsでtar.gz2を解凍するにはいくつかの方法がありますが，私は以下のソフトウェアを使い解凍しました
      * 7 ZIP : http://www.7-zip.org/
      * Lhaplus : http://www7a.biglobe.ne.jp/~schezo/
    * 解凍にもやや時間がかかります．bz2を解凍したら，tarを解凍しなければなりません．

## CDH2の起動 ##
  * VMware Playerを起動します
  * VMware Playerが起動したら，「仮想マシンを開く」をクリックします
  * 先ほど解凍したCDH2のディレクトリの中に，cloudera-training-0.3.3.vmxというファイルがあるので，それを選択して「開く」ボタンをクリックします
    * 仮想マシンを開くときに「移動しました」「コピーしました」という選択画面が現れるので，通常は「コピーしました」を選択します
    * CDH2の起動が開始されます

## CDH2の初期設定 ##
  * ここからCDH2の設定について解説しますが，CDH2には日本語フォントが入っていないため，puttyなどを用いて外部のターミナルから使うことをお勧めします(もちろん日本語フォントを入れれば良いのですが．)
  * CDH2はLinuxのディストリビューションとしてUbuntuを用いています．人によって初期設定はいろいろあるかもしれませんが，jatextminingを使う上で最低限設定しておいた方が良いだろうと思う事項を解説しておきます
    * Cloudera社が提供してる資料もあるので，詳しくはそちらもご参照ください
      * http://www.cloudera.com/downloads/virtual-machine/
  * ログイン
    * CDH2が起動したらログイン画面が現れるマス
    * ログインユーザID training / ログインパスワード training でログインできます
  * workspaceの更新する
    * 上記のCloudera社の解説にもあるように，まずパッケージのworkspaceを更新をします
```
$ cd ~/git
$ ./update-exercises --workspace
```
### 日本語環境を整えます ###
```
sudo aptitude install language-pack-ja
sudo dpkg-reconfigure locales
```
    * ついでにシェルの言語設定もja\_JP.UTF-8にしておきます．
    * .bashrc最後の方の行にLANG=ja\_JP.UTF-8と追記して以下のコマンドを実行します(詳しい方はお好きな方法でやってください)
```
# LANG=ja_JP.UTF-8を追記する
emacs .bashrc
source .bashrc
```

### GoSenのダウンロードと設置 ###

  * subversionをインストールします

```
sudo apt-get install subversion
```

  * GoSenのダウンロードと設置
```
mkdir ~/tmp
cd ~/tmp

# GoSenのチェックアウト
svn co https://itadaki.svn.sourceforge.net/svnroot/itadaki/GoSen GoSen

# /usr/local/に設置．jatextminingではデフォルトの設定ではここに設置することになっていますが，configurationを変更すれば変更可能です
sudo cp -R ~/tmp/GoSen /usr/local/

# GoSenのビルド
cd ~/tmp/GoSen

# パッケージのビルド
ant

# 辞書のビルド
cd testdata/dictionary/
ant

# 動作確認
cd ~/tmp/GoSen/
java -cp $CLASSPATH:bin:gosen-1.0beta.jar examples.StringTaggerDemo testdata/dictionary/dictionary.xml
```


  * GoSenのクラスパスをHadoopのコンフィグに追加する
```
# Hadoopのコンフィグレーションを修正するためにユーザをhadoopに変更します
sudo su hadoop
```
    * Hadoopのコンフィグレーションに以下の行を追加してください
    * Hadoopのコンフィグレーションは/usr/lib/hadoop/conf/hadoop-env.sh です
```
L.12あたり
# Extra Java CLASSPATH elements.  Optional.
# export HADOOP_CLASSPATH=
```

この行を以下のように，修正してください．

```
# Extra Java CLASSPATH elements.  Optional.
export HADOOP_CLASSPATH=$HADOOP_CLASSPATH:/usr/local/GoSen/gosen-1.0beta.jar:/usr/local/GoSen/jisx0213-1.0.jar
```

  * コンフィグレーションの修正を反映させるためにHadoopクラスターを再起動します
    * スーパーユーザーで実行してください
```
  # trainingユーザーの状態で
  sudo su
  for x in /etc/init.d/hadoop-0.20-* ; do ${x} stop ; done
  for x in /etc/init.d/hadoop-0.20-* ; do ${x} start ; done
```

  * 何かテキストをHDFSにputしてみます
```
# 適当なテキストファイルを作成します
training@training-vm:~/tmp$ cat test.txt
今日は晴れています。

# putしてみます
training@training-vm:~/tmp$ hadoop fs -put test.txt test.txt

# 再起動に失敗するとsafemodeになってしまい，HDFSのファイル操作ができなくなってしまいます．
# もし以下のようなメッセージが出た場合はsafemodeになってしまっています
put: org.apache.hadoop.hdfs.server.namenode.SafeModeException: Cannot create file/user/training/test.txt. Name node is in safe mode.

# safemodoを解除するには-safemode leaveを実行します
training@training-vm:~/tmp$ hadoop dfsadmin -safemode leave
Safe mode is OFF

# また，再起動に失敗などすると以下のようなエラーが出てしまう場合もあります
training@training-vm:~/tmp$ hadoop fs -put test.txt test.txt
10/09/15 23:57:11 WARN hdfs.DFSClient: DataStreamer Exception: org.apache.hadoop.ipc.RemoteException: java.io.IOException: File /user/training/test.txt could only be replicated to 0 nodes, instead of 1
        at org.apache.hadoop.hdfs.server.namenode.FSNamesystem.getAdditionalBlock(FSNamesystem.java:1267)
        at org.apache.hadoop.hdfs.server.namenode.NameNode.addBlock(NameNode.java:434)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)

# このようなエラーが出た場合はhttp://localhost:50070にブラウザからアクセスすると治ることがあります

# 無事putできたら内容を確認してみます

training@training-vm:~/tmp$ hadoop fs -cat test.txt
今日は晴れています。
```

# jatextminingのダウンロードと使い方 #

## jatextminigのダウンロードとビルド ##

  * 簡単のためにGoSenの時と同様にsubversionを用いてcheck outします
```
training@training-vm:~/tmp$ svn checkout http://jatextmining.googlecode.com/svn/trunk/ jatextmining-read-only
```

  * jatextminingをビルドします
```
training@training-vm:~/tmp$ cd jatextmining-read-only/

training@training-vm:~/tmp/jatextmining-read-only$ ant
Buildfile: build.xml

init:
    [mkdir] Created dir: /home/training/tmp/jatextmining-read-only/build

compile:
    [javac] Compiling 18 source files to /home/training/tmp/jatextmining-read-only/build

jar:
      [jar] Building jar: /home/training/tmp/jatextmining-read-only/jatextmining-0.1.jar

BUILD SUCCESSFUL
Total time: 4 seconds
```

## wikipediaのダウンロードとHDFSへのコピー ##

  * 実データを使って解析するには，データを用意しなければなりません．
  * 今回は日本語wikipediaのアブストラクトを使ってみたいと思います．
    * http://download.wikimedia.org/jawiki/latest/にある，jawiki-latest-abstract.xmlを用いたいと思います

```
# dataをおくディレクトリを作ります
training@training-vm:~/tmp/jatextmining-read-only$ mkdir data
training@training-vm:~/tmp/jatextmining-read-only$ cd data/

# wikipediaのデータをダウンロードします(自分の環境では1時間ぐらいかかりました．．．)
training@training-vm:~/tmp/jatextmining-read-only/data$ wget http://download.wikimedia.org/jawiki/latest/jawiki-latest-abstract.xml

```

  * XMLであるjawiki-latest-abstract.xmlからアブストラクトのテキスト部分のみを抽出しHDFSにputします
    * ここでは簡単なjatextminingに格納されている簡単なperl script(jatextmining/script/abstract.pl)を用いて文書部分を抽出したいと思います
    * jawiki-latest-abstract.xmlから１万エントリ分のアブストラクトを抽出します
      * １万エントリ分しか使わないのはVMware上の処理のため，あまり多くのデータを処理すると時間がかかためです
      * Ⅰ万エントリでもそこそこ傾向はつかめます
      * 実際に大規模なHadoopクラスタを構築した場合は，全件のアブストラクトでも容易に処理できます

```
# jawiki-latest-abstract.xmlから１万エントリ分のアブストラクトを抽出します
training@training-vm:~/tmp/jatextmining-read-only/data$ perl ../script/abstract.pl < jawiki-latest-abstract.xml > wiki_abstract

# HDFSにwiki_abstractをコピーします
training@training-vm:~/tmp/jatextmining-read-only/data$ hadoop fs -put wiki_abstract wiki_abstract
```

## jatextminingで頻度分析 ##

  * jatextminingを用いてwiki\_abstractのすべての文書から名詞の頻度を抽出します

```
# jatextminingのディレクトリに移動します
training@training-vm:~/tmp/jatextmining-read-only$ ls
AUTHORS  ChangeLog  LICENSE  build  build.xml  conf  data  jatextmining-0.1.jar  lib  script  src

# jatextminingを用いてwiki_abstractから各名詞の頻度をカウントします
training@training-vm:~/tmp/jatextmining-read-only$ hadoop jar jatextmining-0.1.jar wordcount -i wiki_abstract -o wiki_count -p noun
10/09/16 04:43:44 INFO input.FileInputFormat: Total input paths to process : 1
10/09/16 04:43:58 INFO mapred.JobClient: Running job: job_201009152351_0002
10/09/16 04:43:59 INFO mapred.JobClient:  map 0% reduce 0%
10/09/16 04:46:07 INFO mapred.JobClient:  map 49% reduce 0%
10/09/16 04:46:10 INFO mapred.JobClient:  map 100% reduce 0%
10/09/16 04:46:19 INFO mapred.JobClient:  map 100% reduce 100%
10/09/16 04:46:21 INFO mapred.JobClient: Job complete: job_201009152351_0002
10/09/16 04:46:21 INFO mapred.JobClient: Counters: 17
10/09/16 04:46:21 INFO mapred.JobClient:   Job Counters
10/09/16 04:46:21 INFO mapred.JobClient:     Launched reduce tasks=1
10/09/16 04:46:21 INFO mapred.JobClient:     Launched map tasks=1
10/09/16 04:46:21 INFO mapred.JobClient:     Data-local map tasks=1
10/09/16 04:46:21 INFO mapred.JobClient:   FileSystemCounters
10/09/16 04:46:21 INFO mapred.JobClient:     FILE_BYTES_READ=1877791
10/09/16 04:46:21 INFO mapred.JobClient:     HDFS_BYTES_READ=1722620
10/09/16 04:46:21 INFO mapred.JobClient:     FILE_BYTES_WRITTEN=3755614
10/09/16 04:46:21 INFO mapred.JobClient:     HDFS_BYTES_WRITTEN=206171
10/09/16 04:46:21 INFO mapred.JobClient:   Map-Reduce Framework
10/09/16 04:46:21 INFO mapred.JobClient:     Reduce input groups=15877
10/09/16 04:46:21 INFO mapred.JobClient:     Combine output records=0
10/09/16 04:46:21 INFO mapred.JobClient:     Map input records=10000
10/09/16 04:46:21 INFO mapred.JobClient:     Reduce shuffle bytes=1877791
10/09/16 04:46:21 INFO mapred.JobClient:     Reduce output records=15877
10/09/16 04:46:21 INFO mapred.JobClient:     Spilled Records=216248
10/09/16 04:46:21 INFO mapred.JobClient:     Map output bytes=1661537
10/09/16 04:46:21 INFO mapred.JobClient:     Combine input records=0
10/09/16 04:46:21 INFO mapred.JobClient:     Map output records=108124
10/09/16 04:46:21 INFO mapred.JobClient:     Reduce input records=108124
```

  * 結果を確認してみましょう(上位10件を表示してみます)
```
training@training-vm:~/tmp/jatextmining-read-only$ hadoop fs -cat wiki_count/* | sort -n -r -k 2 | head -n 10
cat: Source must be a file.
こと    1526.0
年      1453.0
月      1244.0
日      1209.0
日本    921.0
的      777.0
県      615.0
市      554.0
もの    505.0
家      486.0
```

  * 形容詞の頻度を抽出することもできます
```
# 細かいところは省略して実行方法を示します

training@training-vm:~/tmp/jatextmining-read-only$ hadoop jar jatextmining-0.1.jar wordcount -i wiki_abstract -o wiki_adj -p adj

cat: Source must be a file.
多い    225.0
大きい  59.0
広い    59.0
長い    56.0
よい    46.0
こい    44.0
高い    42.0
くい    37.0
強い    30.0
古い    29.0
```

## jatextminingで共起分析 ##

  * jatextminigを用いてwiki\_abstractのすべての文書から名詞を抽出し，各エントリ中で共起する名詞のペアを分析してみます
  * 共起頻度をカウントします
```
# 名詞の共起頻度を計算
training@training-vm:~/tmp/jatextmining-read-only$ hadoop jar jatextmining-0.1.jar cooccurrence -i wiki_abstract -o wiki_co_freaq -p noun -t freaq

# 結果をファイルに出力
hadoop fs -cat wiki_co_freaq/* > wiki_co_freaq

# 「大阪」の共起単語を出力(実際にはタブ区切りですが，見やすさのために改行区切りに修正しています)
training@training-vm:~/tmp/jatextmining-read-only$ grep "^大阪  " wiki_co_freaq
大阪
路線14.0000
鉄道13.0000
日本13.0000
西日本旅客鉄道12.0000
西日本12.0000
出身10.0000
漫画8.0000
京都8.0000
愛称6.0000
東京6.0000
中央6.0000
幹線6.0000
地下鉄5.0000
市営5.0000
会社5.0000
生まれ5.0000
生地5.0000
現在5.0000
兵庫5.0000
区間5.0000
```

  * χ二乗検定で重み付けする
```
# 共起頻度をχ二乗で重み付けした結果を計算する
training@training-vm:~/tmp/jatextmining-read-onlhadoop jar jatextmining-0.1.jar cooccurrence -i wiki_abstract -o wiki_co_chi -p noun -t chi

# 結果をファイルに出力
hadoop fs -cat wiki_co_chi/* > wiki_co_chi

# 「大阪」の共起単語を出力
training@training-vm:~/tmp/jatextmining-read-only$ grep "^大阪  " wiki_co_chi
大阪
西日本旅客鉄道802.5295
西日本675.6637
浪速496.1985
難波496.1985
おおさか496.1985
天王寺496.1985
南海電気鉄道372.1116
吹田372.1116
阿倍野372.1116
東大阪372.1116
近畿日本鉄道280.2065
豊中277.6131
新大阪248.0496
西九条248.0496
阪神電気鉄道248.0496
城東248.0496
八尾248.0496
フィルハーモニー248.0496
ハイウェイ248.0496
京都245.0262
```

  * χ二乗の方が，単純に頻度を計算するよりも，より関連性の強い単語が上位にきます．
