#!/bin/bash

#项目名
pjnm=$1
#代码路径
Url=$2
#版本号
Revision=$3
#获取当前时间
currentdatetime=$(date +%Y%m%d%H%M%S)
#Tag前置路径
tag_prepath="${Url%/*}/tags/"
#Tag目录
tag_dir="${Url##*/}_tags/"
#build目录
tag_bulid="build_$Revision_$currentdatetime/"

echo "代码路径$Url"
echo "版本号$Revision"
echo "Tag前置路径$tag_prepath"
echo "Tag目录$tag_dir"
echo "build目录$tag_bulid"
echo "$tag_prepath$tag_dir$tag_bulid"
tag_url="$tag_prepath$tag_dir$tag_bulid"
echo $tag_url

#保留最新的5个Tag
for i in `svn ls $tag_prepath$tag_dir --username "用户名" --password "密码" --non-interactive|sort|awk '{L[NR]=$0}END{for (i=1;i<=NR-5;i++){print L[i]}}'`
do
svn delete --force $tag_prepath$tag_dir$i -m '$i' --username "用户名" --password "密码" --non-interactive
echo "Delete $i"
done
echo "Delete is complete!"

echo "创建tag下的版本目录"
svn mkdir --parents $tag_url -m "mkdir tags $pjnm-$Revision$currentdatetime" --username "用户名" --password "密码" --non-interactive

echo "打tag到tags $tag_url目录"

svn copy -r $Revision $Url/ $tag_url -m "$pjnm-$Revision$currentdatetime" --username "用户名" --password "密码" --non-interactive
echo "svn copy -r $Revision $Url/ $tag_url -m "$pjnm-$Revision$currentdatetime" --username "用户名" --password "密码" --non-interactive"
echo "Tag成功-----------------------------------"