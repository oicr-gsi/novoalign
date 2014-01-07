#!/bin/bash

cd $1
qrsh -l h_vmem=8G -cwd ". /oicr/local/Modules/default/init/bash ; module load samtools 2>/dev/null; find . -regex '.*\.bam$' -exec sh -c \" samtools flagstat {} | tr '\n' '\t'; echo \" \; | sort | uniq | tr '\t' '\n'"