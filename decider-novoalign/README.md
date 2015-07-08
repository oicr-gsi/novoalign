##NovoAlign Decider

Version 2.1.2

This decider launches the [Novoalign workflow](../workflow-novoalign) on fastq files with paired reads or single read data. It identifies files for mates (paired-read sequencing) by checking names of the input files. This decider provides a 'nested grouping' feature that allow grouping first by then by template type (geo_library_source_template\_type) and finally by group id (geo_group_id).

*    Runs Novoalign workflow
*    Sets numerous parameters to their default values
*    Provides nesting grouping (by parent, template, and group_id)
*    Runs various checks enforcing proper syntax of the parameters
*    Automatically retrieves and pass into .ini the following variables: ius_accession, sequencer_run_name and lane
*    May intelligently guess the type of experiment (either single-read or paired-read data). The decider relies on the presence of certain flags in the files' names - i.e. '_R1\_'  '1_sequence.txt' '.1.fastq'.


## Dependencies

The [Novoalign workflow](../workflow-novoalign) must first be compiled.

## Compile

    mvn clean install



<table>
  <tbody>
    <tr>
      <th>
        <p>Command</p>
      </th>
      <th>
        <p>Description</p>
      </th>
      <th colspan="1">Default</th>
    </tr>
    <tr>
      <td colspan="1">
        <p>
          <code>--verbose</code>
        </p>
      </td>
      <td colspan="1">output all SeqWare info.</td>
      <td colspan="1"> </td>
    </tr>
    <tr>
      <td colspan="1">
        <p>
          <code>--template-type</code>
        </p>
      </td>
      <td colspan="1">Restrict analysis to a particular template type. One of: CH, EX, MR, SM, TR, TS, WG, WT</td>
      <td colspan="1"> </td>
    </tr>
    <tr>
      <td colspan="1">
        <p>
          <code>--colorspace</code>
        </p>
      </td>
      <td colspan="1">Whether to run with colorspace for Novoalign. One of 0 (false) or 1 (true)</td>
      <td colspan="1">0</td>
    </tr>
    <tr>
      <td colspan="1">
        <p>
          <code>--run-ends</code>
        </p>
      </td>
      <td colspan="1">Run ends will define if it is Single-End(1) or Paired-End(2) experiment,</td>
      <td colspan="1">the decider should figure out this automatically</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--novoalign-slots</pre>
      </td>
      <td colspan="1">Novoalign slots</td>
      <td colspan="1">1</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--novoalign-memory</pre>
      </td>
      <td colspan="1">Novoalign memory in MB</td>
      <td colspan="1">16000</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--novoalign-threads</pre>
      </td>
      <td colspan="1">Novoalign threads including flag -c</td>
      <td colspan="1">-c 8</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--novoalign-input-format</pre>
      </td>
      <td colspan="1">
        <p>The format of the fastq file quality scores. If left blank, Novoalign guesses using the first few thousand bases. e.g. -F ILMFQ</p>
      </td>
      <td colspan="1">-</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--novoalign-index</pre>
      </td>
      <td colspan="1">index generated with Novoindex reference for reference genome, default is is hg19_random.nix.</td>
      <td colspan="1">-d ${workflow_bundle_dir}/Workflow_Bundle_GenomicAlignmentNovoalign/0.10.4/data/indexes/novoalign/hg19/hg19_random/hg19_random.nix</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--novoalign-expected-insert</pre>
      </td>
      <td colspan="1">Novoalign expected insert</td>
      <td colspan="1">-i PE 250,50</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--novoalign-r1-adapter-trim</pre>
      </td>
      <td colspan="1">Novoalign r1 adapter trim including -a tag.</td>
      <td colspan="1">-a AGATCGGAAGAGCGGTTCAGCAGGAATGCCGAGACCG</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--novoalign-r2-adapter-trim</pre>
      </td>
      <td colspan="1">Novoalign r2 adapter trim excluding -a tag</td>
      <td colspan="1">AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGT</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--novoalign-additional-parameters</pre>
      </td>
      <td colspan="1">Novoalign additional parameters</td>
      <td colspan="1">-r ALL 5 -R 0 -o SAM</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--picard-threads</pre>
      </td>
      <td colspan="1">Picard threads</td>
      <td colspan="1">1</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--picard-slots</pre>
      </td>
      <td colspan="1">Picard slots</td>
      <td colspan="1">1</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--picard-memory</pre>
      </td>
      <td colspan="1">Picard memory in MB</td>
      <td colspan="1">3000</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--picardmerge-slots</pre>
      </td>
      <td colspan="1">Picard merge slots</td>
      <td colspan="1">1</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--rg-library</pre>
      </td>
      <td colspan="1">Read Group library</td>
      <td colspan="1">library</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--rg-platform</pre>
      </td>
      <td colspan="1">Sequencing platform</td>
      <td colspan="1">illumina</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--rg-platform-unit</pre>
      </td>
      <td colspan="1">Sequencing platform unit</td>
      <td colspan="1">flowcell-barcode_lane</td>
    </tr>
    <tr>
      <td colspan="1">
        <pre>--rg-sample-name</pre>
      </td>
      <td colspan="1">Sample name</td>
      <td colspan="1">sample</td>
    </tr>
    <tr>
      <td>
        <p>
          <code>--output-folder</code>
        </p>
      </td>
      <td>
        <p>the name of the folder to put the output into relative to the output-path. Corresponds to output-dir in INI file</p>
      </td>
      <td> seqware-results</td>
    </tr>
    <tr>
      <td>
        <p>
          <code>--output-path</code>
        </p>
      </td>
      <td>
        <p>the path where the files should be copied to after analysis. Corresponds to output-prefix in INI file</p>
      </td>
      <td> ./</td>
    </tr>
    <tr>
      <td>
        <pre>--barcode</pre>
      </td>
      <td>barcode info</td>
      <td>NoIndex</td>
    </tr>
    <tr>
      <td>
        <pre>--queue</pre>
      </td>
      <td>queue priority on cluster</td>
      <td>production</td>
    </tr>
    <tr>
      <td colspan="1"> </td>
      <td colspan="1"> </td>
      <td colspan="1"> </td>
    </tr>
  </tbody>
</table>
