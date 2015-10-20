##NovoAlign Decider

Version 2.1.3

###Overview

This decider launches the [Novoalign workflow](../workflow-novoalign) on fastq files with paired reads or single read data. It identifies files for mates (paired-read sequencing) by checking names of the input files. This decider provides a 'nested grouping' feature that allow grouping first by then by template type (geo_library_source_template\_type) and finally by group id (geo_group_id).

*    Runs Novoalign workflow
*    Sets numerous parameters to their default values
*    Provides nesting grouping (by parent, template, and group_id)
*    Runs various checks enforcing proper syntax of the parameters
*    Automatically retrieves and pass into .ini the following variables: ius_accession, sequencer_run_name and lane
*    May intelligently guess the type of experiment (either single-read or paired-read data). The decider relies on the presence of certain flags in the files' names - i.e. '_R1\_'  '1_sequence.txt' '.1.fastq'.

Please see [basic deciders](https://seqware.github.io/docs/6-pipeline/basic_deciders) for additional information.

###Compile

```
mvn clean install
```

###Usage

```
java -jar Decider.jar --study-name <study-name> --wf-accession <novoalign-workflow-accession>
```

####Options

**Required**

Parameter                        | Type     | Description
---                              | ---      | ---
wf-accession                     | int      | Novoalign workflow accession

**Filters**

**--all** or at least one of (providing multiple arguments by repeating parameter, e.g. --study-name Study1 --study-name Study2):
    
    lane-SWID
    ius-SWID
    study-name
    sample-name
    root-sample-name
    sequencer-run-name
    organism
    workflow-run-status
    processing-status
    processing-SWID

Optional Filters:

Parameter                        | Type     | Description
---                              | ---      | ---
template-type                    | string   | Restrict analysis to a particular template type. One of: CH, EX, MR, SM, TR, TS, WG, WT

Optional:

Parameter                        | Type     | Description
---                              | ---      | ---
verbose                          |          | Output all SeqWare info
colorspace                       | true|false | Whether to run with colorspace for Novoalign
run-ends                         | int      | Run ends will define if it is Single-End(1) or Paired-End(2) experiment (the decider should figure out this automatically)
novoalign-slots                  | int      | Novoalign slots
novoalign-memory                 | int      | Novoalign memory in MB
novoalign-threads                | int      | Novoalign threads including flag -c
novoalign-input-format           | string   | The format of the fastq file quality scores. If left blank, Novoalign guesses using the first few thousand bases (e.g. -F ILMFQ)
novoalign-index                  | path     | Index generated with Novoindex reference for reference genome (e.g. /data/hg19_random.nix)
novoalign-expected-insert        | string   | Novoalign expected insert (e.g. -i PE 250,50)
novoalign-r1-adapter-trim        | string   | Novoalign r1 adapter trim including -a tag (e.g. -a AGATCGGAAGAGCGGTTCAGCAGGAATGCCGAGACCG)
novoalign-r2-adapter-trim        | string   | Novoalign r2 adapter trim excluding -a tag (e.g. AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGT)
novoalign-additional-parameters  | string   | Novoalign additional parameters (e.g. -r ALL 5 -R 0 -o SAM)
picard-threads                   | int      | Picard threads
picard-slots                     | int      | Picard slots
picard-memory                    | int      | Picard memory in MB
picardmerge-slots                | int      | Picard merge slots
rg-library                       | string   | Read Group library
rg-platform                      | string   | Sequencing platform
rg-platform-unit                 | string   | Sequencing platform unit
rg-sample-name                   | string   | Sample name
barcode                          | string   | Barcode info (e.g. NoIndex)
queue                            | string   | Queue on cluster (e.g. production)
output-folder                    | string   | The name of the folder to put the output into relative to the output-path (e.g. seqware-results)
output-path                      | path     | The path where the files should be copied to after analysis. (e.g. /my/output/dir/)

##Support

For support, please file an issue on the Github project or send an email to gsi@oicr.on.ca .
