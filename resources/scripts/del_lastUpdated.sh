#!/bin/bash

# 请理maven本地仓库.lastUpdated的文件。
# 本地仓库的地址
del_path="/home/repository/"

find $del_path -name '*.lastUpdated' -print |xargs rm -f

echo "是否还有残留"
find $del_path -name '*.lastUpdated' -print
