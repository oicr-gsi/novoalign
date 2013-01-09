<?xml version="1.0" encoding="UTF-8"?>
<adag xmlns="http://pegasus.isi.edu/schema/DAX" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://pegasus.isi.edu/schema/DAX http://pegasus.isi.edu/schema/dax-3.2.xsd" version="3.2" count="1" index="0" name="GenomicAlignmentNovoalign_0.10.4">

<!--

DESCRIPTION:

This workflow is intended to align a fastq or series of fastq files. It is capable to doing the following:

* align fastq to a reference using Novoalign for each fastq input
* annotating BAMs with read group (RG) information including library and sample name

Generally this workflow should be run on a per IUS basis or, if multiple input fastqs are 
selected, they should all be the same sample and library since this populates the read group
fields in the BAM

FIXME: colorspace or nucleotide space flag
FIXME: need to switch to public HTTP URIs for bundles (see ini file)
FIXME: need to start with BAM file rather than fastq

-->

<!-- the directory structure inside the bundle -->
<#assign workflow_name = "Workflow_Bundle_GenomicAlignmentNovoalign/0.10.4"/>

<!-- MACRO: to create a mkdir pre job and stage mkdir binary -->
<#macro requires_dir dir>
  <profile namespace="env" key="GRIDSTART_PREJOB">/${workflow_bundle_dir}/${workflow_name}/bin/pegasus-dirmanager -c -d ${dir}</profile>
</#macro>

<!-- VARS -->
<#-- workflow and seqware versions -->
<#assign seqware_version = "0.13.5"/>
<#assign workflow_version = "0.10.4"/>
<#assign novocraft_version = "V2.07.14"/>
<#-- make sure it is a string -->
<#assign parentAccessions = "${parent_accession}"/>
<#assign runends = "${run_ends}"/>
<#assign inputs_e1="${fastq_inputs_end_1}"/>
<#if (run_ends > 1 )>
<#assign inputs_e2="${fastq_inputs_end_2}"/>
</#if>
<#-- Set relative paths for files within the run-->
<#assign bin_dir = "bin"/>
<#assign data_dir = "data"/>
<#assign lib_dir = "lib"/>


<!-- EXECUTABLES INCLUDED WITH BUNDLE -->
<executable namespace="seqware" name="runner" version="${seqware_version}" 
            arch="x86_64" os="linux" installed="true" >
  <!-- the path to the tool that actually runs a given module -->
  <pfn url="file:///${workflow_bundle_dir}/${workflow_name}/bin/seqware-java-wrapper.sh" site="${seqware_cluster}"/>     
</executable>

<executable namespace="pegasus" name="dirmanager" version="1" 
            arch="x86_64" os="linux" installed="true" >
  <!-- the path to the tool that actually runs a given module -->
  <pfn url="file:///${workflow_bundle_dir}/${workflow_name}/bin/pegasus-dirmanager" site="${seqware_cluster}"/>     
</executable>


<!-- Part 1: Define all jobs -->

<!-- PROVISION -->

  <!-- Pre Job: makes novoalign binaries available -->
  <job id="IDPRE0" namespace="seqware" name="runner" version="${seqware_version}">
   <argument>
      -Xmx1000M
	  -classpath ${workflow_bundle_dir}/${workflow_name}/classes:${workflow_bundle_dir}/${workflow_name}/lib/seqware-distribution-${seqware_version}-full.jar
      net.sourceforge.seqware.pipeline.runner.Runner
      --no-metadata
      --module net.sourceforge.seqware.pipeline.modules.utilities.ProvisionDependenciesBundle
      --
      --input-file ${workflow_bundle_dir}/${workflow_name}/dependencies/x86_64/novocraft${novocraft_version}.x86_64.zip
      --output-dir ${bin_dir}
    </argument>

    <profile namespace="globus" key="jobtype">condor</profile>
    <profile namespace="globus" key="count">1</profile>
    <profile namespace="globus" key="maxmemory">2000</profile>

  </job>

  <!-- Pre Job: makes picard available -->
  <job id="IDPRE1" namespace="seqware" name="runner" version="${seqware_version}">
    <argument>
      -Xmx1000M
	  -classpath ${workflow_bundle_dir}/${workflow_name}/classes:${workflow_bundle_dir}/${workflow_name}/lib/seqware-distribution-${seqware_version}-full.jar
      net.sourceforge.seqware.pipeline.runner.Runner
      --no-metadata
      --module net.sourceforge.seqware.pipeline.modules.utilities.ProvisionDependenciesBundle
      --
      --input-file ${workflow_bundle_dir}/${workflow_name}/dependencies/noarch/picard-tools-1.48.noarch.zip
      --output-dir ${bin_dir}
    </argument>

    <profile namespace="globus" key="jobtype">condor</profile>
    <profile namespace="globus" key="count">1</profile>
    <profile namespace="globus" key="maxmemory">2000</profile>

  </job>

  <!-- Pre Job: makes seqware perl scripts available -->
  <job id="IDPRE2" namespace="seqware" name="runner" version="${seqware_version}">
    <argument>
      -Xmx1000M
	  -classpath ${workflow_bundle_dir}/${workflow_name}/classes:${workflow_bundle_dir}/${workflow_name}/lib/seqware-distribution-${seqware_version}-full.jar
      net.sourceforge.seqware.pipeline.runner.Runner
      --no-metadata
      --module net.sourceforge.seqware.pipeline.modules.utilities.ProvisionDependenciesBundle
      --
      --input-file ${workflow_bundle_dir}/${workflow_name}/dependencies/noarch/seqware-pipeline-perl-bin.noarch.zip
      --output-dir ${bin_dir}
    </argument>

    <profile namespace="globus" key="jobtype">condor</profile>
    <profile namespace="globus" key="count">1</profile>
    <profile namespace="globus" key="maxmemory">2000</profile>

  </job>

<!-- MODULE CALLS -->
<!-- Iterate over all input files (fastq or input) -->
<#list inputs_e1?split(",") as input_e1>

  <#-- Set the basename from input name, removing .input -->
  <#list input_e1?split("/") as tmp>
    <#assign basename_e1 = tmp/>
  </#list>

  <#if (run_ends > 1)>

    <#assign inputs_e2_arr = inputs_e2?split(",") />
    <#assign input_e2 = inputs_e2_arr[input_e1_index]/>
    <#-- Set the basename from input name, removing .input -->
    <#list input_e2?split("/") as tmp>
      <#assign basename_e2 = tmp/>
    </#list>

  </#if>

  <!-- Job: figure out if the input is a URL and, if it is, correclty download it to a staging area otherwise link it -->
  <job id="ID0.${input_e1_index}" namespace="seqware" name="runner" version="${seqware_version}">
    <argument>
      -Xmx1000M
	  -classpath ${workflow_bundle_dir}/${workflow_name}/classes:${workflow_bundle_dir}/${workflow_name}/lib/seqware-distribution-${seqware_version}-full.jar
      net.sourceforge.seqware.pipeline.runner.Runner
      --no-metadata
      --module net.sourceforge.seqware.pipeline.modules.utilities.ProvisionFiles
      --
      --input-file ${input_e1}
      <#if (run_ends > 1)> --input-file ${input_e2} </#if>
      --output-dir ${data_dir}
    </argument>

    <profile namespace="globus" key="jobtype">condor</profile>
    <profile namespace="globus" key="count">1</profile>
    <profile namespace="globus" key="maxmemory">2000</profile>

  </job>

  <!-- Job: novoalign -->
  <#assign module = "net.sourceforge.seqware.pipeline.modules.GenericCommandRunner"/>
  <#assign algo = "Novoalign"/>
  <job id="ID1.${input_e1_index}" namespace="seqware" name="runner" version="${seqware_version}">
    <argument>
      -Xmx1000M
	  -classpath ${workflow_bundle_dir}/${workflow_name}/classes:${workflow_bundle_dir}/${workflow_name}/lib/seqware-distribution-${seqware_version}-full.jar
      net.sourceforge.seqware.pipeline.runner.Runner
      --${metadata}
      <#list parentAccessions?split(",") as pa>
      --metadata-parent-accession ${pa}
      </#list>
      --metadata-processing-accession-file ${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${algo}_${input_e1_index}_accession
      --metadata-output-file-prefix ${output_prefix}
      --metadata-workflow-run-accession ${workflow_run_accession}
      --module ${module}
      --
      --gcr-algorithm ${algo}
      --gcr-command ${bin_dir}/novocraft${novocraft_version}/novoalign
<#if (run_ends > 1)>
      -f ${data_dir}/${basename_e1} ${data_dir}/${basename_e2} ${novoalign_expected_insert} ${novoalign_r1_adapter_trim} ${novoalign_r2_adapter_trim}
<#else>
      -f ${data_dir}/${basename_e1} ${novoalign_r1_adapter_trim}
</#if>
      ${novoalign_index}
	  ${novoalign_input_format} ${novoalign_threads} ${novoalign_additional_parameters} > ${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}/${basename_e1}.sam
    </argument>

    <!-- See http://www.globus.org/api/c-globus-4.0.3/globus_gram_job_manager/html/globus_job_manager_rsl.html -->
    <!-- See http://pegasus.isi.edu/wms/docs/3.0/advanced_concepts_profiles.php#id2738647 -->
    <profile namespace="globus" key="jobtype">condor</profile>
    <profile namespace="globus" key="count">${novoalign_slots}</profile>
    <profile namespace="globus" key="maxmemory">${novoalign_memory}</profile>

    <!-- Prejob to make output directory -->
    <@requires_dir "${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}"/>

  </job>

  <!-- Job: picard to create BAM -->
  <#assign parentAlgo = "${algo}"/>
  <#assign algo = "PicardConvertNovoalign"/>
  <#assign module = "net.sourceforge.seqware.pipeline.modules.GenericCommandRunner"/>
  <job id="ID4.${input_e1_index}" namespace="seqware" name="runner" version="${seqware_version}">
    <argument>
      -Xmx1000M
	  -classpath ${workflow_bundle_dir}/${workflow_name}/classes:${workflow_bundle_dir}/${workflow_name}/lib/seqware-distribution-${seqware_version}-full.jar
      net.sourceforge.seqware.pipeline.runner.Runner
      --${metadata}
      --metadata-parent-accession-file ${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${parentAlgo}_${input_e1_index}_accession
      --metadata-processing-accession-file ${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${algo}_${input_e1_index}_accession
      --metadata-output-file-prefix ${output_prefix}
      --metadata-workflow-run-ancestor-accession ${workflow_run_accession}
      --module ${module}
      --
      --gcr-algorithm ${algo}
      --gcr-command ${java}
      -Xmx${picard_memory}M
      -jar ${bin_dir}/${picardconvert}
      INPUT=${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}/${basename_e1}.sam
      OUTPUT=${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}/${basename_e1}.presort.bam
      VALIDATION_STRINGENCY=SILENT TMP_DIR=${tmp_dir}
    </argument>

    <!-- See http://www.globus.org/api/c-globus-4.0.3/globus_gram_job_manager/html/globus_job_manager_rsl.html -->
    <!-- See http://pegasus.isi.edu/wms/docs/3.0/advanced_concepts_profiles.php#id2738647 -->
    <profile namespace="globus" key="jobtype">condor</profile>
    <profile namespace="globus" key="count">${picard_slots}</profile>
    <profile namespace="globus" key="maxmemory">${picard_memory + 2000}</profile>

  </job>


  <!-- Job: picard to sort BAM -->
  <#assign parentAlgo = "${algo}"/>
  <#assign algo = "PicardSortNovoalign"/>
  <#assign module = "net.sourceforge.seqware.pipeline.modules.GenericCommandRunner"/>
  <job id="ID5.${input_e1_index}" namespace="seqware" name="runner" version="${seqware_version}">
    <argument>
      -Xmx1000M
	  -classpath ${workflow_bundle_dir}/${workflow_name}/classes:${workflow_bundle_dir}/${workflow_name}/lib/seqware-distribution-${seqware_version}-full.jar
      net.sourceforge.seqware.pipeline.runner.Runner
      --${metadata}
      --metadata-parent-accession-file ${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${parentAlgo}_${input_e1_index}_accession
      --metadata-processing-accession-file ${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${algo}_${input_e1_index}_accession
      --metadata-output-file-prefix ${output_prefix}
      --metadata-workflow-run-ancestor-accession ${workflow_run_accession}
      --module ${module}
      --
      --gcr-algorithm ${algo}
      --gcr-command ${java}
      -Xmx${picard_memory}M
      -jar ${bin_dir}/${picardsort}
      INPUT=${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}/${basename_e1}.presort.bam
      OUTPUT=${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}/${basename_e1}.sorted.bam
      SORT_ORDER=coordinate
      VALIDATION_STRINGENCY=SILENT TMP_DIR=${tmp_dir}
    </argument>

    <!-- See http://www.globus.org/api/c-globus-4.0.3/globus_gram_job_manager/html/globus_job_manager_rsl.html -->
    <!-- See http://pegasus.isi.edu/wms/docs/3.0/advanced_concepts_profiles.php#id2738647 -->
    <profile namespace="globus" key="jobtype">condor</profile>
    <profile namespace="globus" key="count">${picard_slots}</profile>
    <profile namespace="globus" key="maxmemory">${picard_memory + 2000}</profile>

  </job>

  <!-- Job: picard to add read group information to BAM -->
  <#assign parentAlgo = "${algo}"/>
  <#assign algo = "PicardAddReadGroups"/>
  <#assign module = "net.sourceforge.seqware.pipeline.modules.GenericCommandRunner"/>
  <job id="ID6.${input_e1_index}" namespace="seqware" name="runner" version="${seqware_version}">
    <argument>
      -Xmx1000M
      -classpath ${workflow_bundle_dir}/${workflow_name}/classes:${workflow_bundle_dir}/${workflow_name}/lib/seqware-distribution-${seqware_version}-full.jar
      net.sourceforge.seqware.pipeline.runner.Runner
      --${metadata}
      --metadata-parent-accession-file ${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${parentAlgo}_${input_e1_index}_accession
      --metadata-processing-accession-file ${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${algo}_${input_e1_index}_accession
      --metadata-output-file-prefix ${output_prefix}
      --metadata-workflow-run-ancestor-accession ${workflow_run_accession}
      --module ${module}
      --
      --gcr-algorithm ${algo}
      --gcr-output-file PicardAddReadGroups::application/bam::${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}/${basename_e1}.annotated.bam
      --gcr-command ${java}
      -Xmx${picard_memory}M
      -jar ${bin_dir}/${picardrg}
      INPUT=${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}/${basename_e1}.sorted.bam
      OUTPUT=${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}/${basename_e1}.annotated.bam
      SORT_ORDER=coordinate
      VALIDATION_STRINGENCY=SILENT TMP_DIR=${tmp_dir}
      RGID=SWID:${workflow_run_accession}:${input_e1_index}
      RGLB=${rg_library}
      RGPL=${rg_platform}
      RGPU=${rg_platform_unit}
      RGSM=${rg_sample_name}
    </argument>

    <!-- See http://www.globus.org/api/c-globus-4.0.3/globus_gram_job_manager/html/globus_job_manager_rsl.html -->
    <!-- See http://pegasus.isi.edu/wms/docs/3.0/advanced_concepts_profiles.php#id2738647 -->
    <profile namespace="globus" key="jobtype">condor</profile>
    <profile namespace="globus" key="count">${picard_slots}</profile>
    <profile namespace="globus" key="maxmemory">${picard_memory + 2000}</profile>

  </job>

  <!-- Job: copy the output to the correct location -->
  <job id="ID7.${input_e1_index}" namespace="seqware" name="runner" version="${seqware_version}">
    <argument>
      -Xmx1000M
	  -classpath ${workflow_bundle_dir}/${workflow_name}/classes:${workflow_bundle_dir}/${workflow_name}/lib/seqware-distribution-${seqware_version}-full.jar
      net.sourceforge.seqware.pipeline.runner.Runner
      --no-metadata
      --module net.sourceforge.seqware.pipeline.modules.utilities.ProvisionFiles
      --
      --force-copy
      --input-file ${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}/${basename_e1}.annotated.bam
      --output-dir ${output_prefix}${output_dir}/seqware-${seqware_version}_GenomicAlignmentNovoalign-${workflow_version}/${random}
    </argument>

  </job>

</#list>

<!-- End of Job Definitions -->

<!-- Part 2: list of control-flow dependencies -->

  <!-- Define task group dependencies -->

<#list inputs_e1?split(",") as input_e1>
    <child ref="ID0.${input_e1_index}">
      <parent ref="IDPRE0"/>
    </child>
    <child ref="ID0.${input_e1_index}">
      <parent ref="IDPRE1"/>
    </child>
    <child ref="ID0.${input_e1_index}">
      <parent ref="IDPRE2"/>
    </child>
    <child ref="ID1.${input_e1_index}">
      <parent ref="ID0.${input_e1_index}"/>
    </child>
    <child ref="ID4.${input_e1_index}">
      <parent ref="ID1.${input_e1_index}"/>
    </child>
    <child ref="ID5.${input_e1_index}">
      <parent ref="ID4.${input_e1_index}"/>
    </child>
    <child ref="ID6.${input_e1_index}">
      <parent ref="ID5.${input_e1_index}"/>
    </child>
    <child ref="ID7.${input_e1_index}">
      <parent ref="ID6.${input_e1_index}"/>
    </child>
</#list>

<!-- End of Dependencies -->

</adag>
