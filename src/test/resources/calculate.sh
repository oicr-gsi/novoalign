#/bin/bash
cd $1
 

#find . -type f -name fastqc_report.html -exec sed -i '/<div id="header_filename">/,/<\/div>/{/<div id="header_filename">/!{/<\/div>/!d}}' {} \;

#find . -type f -name "*.bai" -exec 
				if [[ -s $(find . -type f -name "*.bai") ]]; then
					exit 1;
				fi
			
				

find . -type f -name SWID_1234_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.bam -exec samtools view -h -o SWID_1234_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.sam {} \;
sed -i 's/UQ:\S*/REMOVED/g' SWID_1234_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.sam;
sed -i 's/AS:\S*/REMOVED/g' SWID_1234_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.sam;
sed -i 's/PQ:\S*/REMOVED/g' SWID_1234_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.sam;
sed -i 's/provisionfiles\S*/REMOVED/g' SWID_1234_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.sam;

find . -type f -name SWID_1235_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.bam -exec samtools view -h -o SWID_1235_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.sam {} \;
sed -i 's/UQ:\S*/REMOVED/g' SWID_1235_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.sam;
sed -i 's/AS:\S*/REMOVED/g' SWID_1235_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.sam;
sed -i 's/PQ:\S*/REMOVED/g' SWID_1235_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.sam;
sed -i 's/provisionfiles\S*/REMOVED/g' SWID_1235_library_121005_h804_0096_AD0V4NACXX_NOINDEX_L005_R1_001.annotated.sam;

find . -type f -name "*.log" -exec sed -i 's/provisionfiles\S*/tempDirNameRemoved/g' {} \;
find . -type f -name "*.log" -exec sed -i '/Starting at/d' {} \;
find . -type f -name "*.log" -exec sed -i '/Elapsed Time/d' {} \;
find . -type f -name "*.log" -exec sed -i '/Done at/d' {} \;
 
find . -type f -name "*.log" -exec md5sum {} +
find . -type f -name "*.sam" -exec md5sum {} +

