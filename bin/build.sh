#!/usr/bin/env bash

cur_dir=$(cd "$(dirname "$0")"; pwd)
# clean jars
jar_dir=$cur_dir/../jars
if [ ! -d $jar_dir ]; then
  mkdir $jar_dir
fi
rm -rf $jar_dir/*

# build modules and copy jars
modules=("svm" "random-forest")
for module in ${modules[@]}; do
    echo "building module ${module}..."
    module_dir=${cur_dir}/../${module}
    target_dir=$module_dir/target
    cd $module_dir
    rm -rf $target_dir
    mvn package
    cp -r $target_dir/*.jar $jar_dir
    echo "jars in ${target_dir} copied..."
done

echo "build finished."