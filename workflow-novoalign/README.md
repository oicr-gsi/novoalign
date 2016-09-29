##NovoAlign workflow

Version 2.3

###Overview

Uses gzipped paired-end fastq files to produce [BAM](http://samtools.github.io/hts-specs/SAMv1.pdf) files using [SeqWare](http://seqware.github.io/) and [NovoAlign](http://www.novocraft.com/products/novoalign/).

![NovoAlign flowchart](docs/novoalign.png)

###Dependencies

This workflow requires:

* [SeqWare](http://seqware.github.io/)
* A Novoalign reference index for versions 2.07.15b or 3.02.04 (the bundled versions)

If you would like to use trimming, the following also must be installed:
* [cutadapt](https://code.google.com/p/cutadapt/)
* [python 2.7](https://www.python.org/download/releases/2.7/)

For testing purposes, some simulated data is available here: http://labs.oicr.on.ca/genome-sequence-informatics/test-data
 
###Compile

Without trimming and novoalign 2:

    mvn clean install -Dnovoalign2-index=/path/to/novoalign2/hg19.nix 


With trimming and novoalign 2:

    mvn clean install -Dnovoalign2-index=/path/to/novoalign2/hg19.nix \
                      -Dcutadapt-path=/path/to/bin/cutadapt           \
                      -Dpython-path=/path/to/bin/python2.7


If using novoalign 3, substitute novoalign2-index for novoalign3-index in the command.

###Usage
After compilation, [test](http://seqware.github.io/docs/3-getting-started/developer-tutorial/#testing-the-workflow), [bundle](http://seqware.github.io/docs/3-getting-started/developer-tutorial/#packaging-the-workflow-into-a-workflow-bundle) and [install](http://seqware.github.io/docs/3-getting-started/admin-tutorial/#how-to-install-a-workflow) the workflow using the techniques described in the SeqWare documentation.

INI files for both Novoalign versions are available and should be used explicitly to match the compilation (e.g. if you specified novoalign2, use the version 2 INI file).

####Options
These parameters can be overridden either in the INI file on on the command line using `--override` when [directly scheduling workflow runs](http://seqware.github.io/docs/3-getting-started/user-tutorial/#listing-available-workflows-and-their-parameters) (not using a decider). Defaults are in [square brackets].

**Required**

Parameter           | Type   | Description
----------          |------  |------------
fastq_inputs_end_1  | path   | The location of fastq read 1 in gzipped fastq. Can be a comma-separated list of read 1 fastqs
fastq_inputs_end_2  | path   | The location of fastq read 2 in gzipped fastq. Can be a comma-separated list of read 2 fastqs.
rg_library          | string | Read group sample library name [library]
rg_platform         | string | Read group sequencing platform name  [illumina]
rg_platform\_unit   | string | Read group flowcell, barcode and lane [flowcell-barcode_lane]
rg_sample_name      | string | Read group sample name [sample]
novoalign\_index    | path   | The path to the Novoalign index for the human genome reference file, including -d flag. [-d hg19_random.nix]

**Novoalign configuration**

Parameter           | Type   | Description
----------          |------  |------------
run_ends                  |int        |The number of reads from the run. Can be 1 or 2 [2]
colorspace                |0 or 1     |Whether the run is in basespace (0) or colorspace (1) [0]
novoalign_r1_adapter_trim |string     |The adapter used for trimming read 1, including the -a flag. Set to "" for no trimming.|["-a AGATCGGAAGAGCGGTTCAGCAGGAATGCCGAGACCG"]
novoalign_r2_adapter\_trim|string     |The adapter used for trimming read 2, not including the -a flag. Set to "" for no trimming. If this is specified, novoalign_r1_adapter_trim must be specified as well. [AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGT]
novoalign_expected_insert |string     |Set the expected read type (e.g. PE), insert size and distribution ["-i PE 250,50"]
novoalign_input_format    |string     |Force quality score format, including -F flag, .e.g "-F STDFQ" [""]
novoalign_additional_parameters|string|Any additional parameters to pass to Novoalign.["-R 0 -o SAM"]

**CutAdapt configuration**

Parameter           | Type   | Description
----------          |------  |------------
do_trim             |boolean | Whether or not to trim the read in the fasta [true]
trim_min_quality    |integer | Minimum quality threshold for trimming
trim_min_length     |integer | Minimum permissible length for reads
r1_adapter_trim     |string  | adapter sequence to trim from read 1 [AGATCGGAAGAGCGGTTCAGCAGGAATGCCGAGACCG]
r2_adapter_trim     |string  | adapter sequence to trim from read 2 [AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGT]
cutadapt_r1_other_params |string | Any other parameters to add to read 1 cutadapt command
cutadapt_r2_other_params | string | Any other parameters to add to read 2 cutadapt command

**Memory/HPC configuration**

Parameter           | Type   | Description
----------          |------  |------------
novoalign_threads|string|The number of threads to give to Novoalign as well as the flag -c. Set to "" for no threading. [""]
novoalign_memory|int|The amount of memory to give to the cluster node running Novoalign in MB. [24000]
novoalign_slots|int|The number of slots to allocate on the cluster node running Novoalign. [1]
picard_threads|int|The number of threads to give to Picard. [1]
picard_slots|int|The number of slots to allocate on the cluster node running Picard. [1]
picard_memory|int|The amount of memory to allocate to the cluster node running Picard [3000]
trim_mem_mb|int|The amount of memory to allocate to the cluster node running cutadapt [16000]
queue|string|The queue to submit to in SGE [production]

**Input/output**

Parameter           | Type   | Description
----------          |------  |------------
ius_accession|string|The comma-separated accessions of the IUS (barcode) from the SeqWare metaDB [1234,1235]
sequencer_run\_name|string|The comma-separated names of the sequencer runs, corresponding to the fastq inputs.[121005_h804_0096_AD0V4NACXX,121005_h804_0096_AD0V4NACXX]
lane|string|The comma-separated lane numbers corresponding to the fastq inputs. [5,5]
barcode|string|The comma-separated barcodes, corresponding to the fastq inputs. [NOINDEX,NOINDEX]
output_prefix | dir| The root output directory
output\_dir| string |     The sub-directory of output_prefix where the output files will be moved
manual\_output | true or false | When false, a random integer will be inserted into the path of the final file in order to ensure uniqueness. When true, the output files will be moved to the location of output_prefix/output_dir [false]


###Output files

* BAM file - aligned, compressed sequence
* BAI file - index file for corresponding BAM file
* log file - standard output from Novoalign. Contains statistics and information about the alignment

###Support
For support, please file an issue on the [Github project](https://github.com/oicr-gsi) or send an email to gsi@oicr.on.ca .
