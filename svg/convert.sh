#!/bin/bash

files=`ls $1`

for file in $files; do
	echo "converting: $file"
	prefix=${file%.*}
	postfix=${file#*.}
	
	if [ $postfix = "xml" ] && [ ! -f ./$prefix.pdf ]; then
		data=`xsltproc xml2svg.xsl $1/$file`
		repData=`echo $data | sed 's/\#[0-9A-Fa-f]\{2\}\([0-9A-Fa-f]\{6\}\)/#\1/g'`
		echo $repData > $prefix.svg
		rsvg-convert -f pdf $prefix.svg > $prefix.pdf
	elif [ $postfix = "png" ] && [ ! -f ./$file ]; then
		cp $1/$file ./$file
	fi
done
