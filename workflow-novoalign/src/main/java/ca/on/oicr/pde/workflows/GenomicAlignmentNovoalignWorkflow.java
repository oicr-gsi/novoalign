/**
 *  Copyright (C) 2015  Ontario Institute of Cancer Research
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact us:
 * 
 *  Ontario Institute for Cancer Research  
 *  MaRS Centre, West Tower
 *  661 University Avenue, Suite 510
 *  Toronto, Ontario, Canada M5G 0A3
 *  Phone: 416-977-7599
 *  Toll-free: 1-866-678-6427
 *  www.oicr.on.ca
**/

package ca.on.oicr.pde.workflows;

import ca.on.oicr.pde.utilities.workflows.OicrWorkflow;
import java.io.File;
import java.util.Map;
import net.sourceforge.seqware.pipeline.workflowV2.model.Command;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import net.sourceforge.seqware.pipeline.workflowV2.model.Workflow;
/**
 * 
 * @author pruzanov, mtaschuk
 */
public class GenomicAlignmentNovoalignWorkflow extends OicrWorkflow {
    
    private static final String DATA_DIR="data_dir";
    private static final String BIN_DIR="bin";
    
    private String dataDir;
    private String queue;
    private int runEnds;
    private SqwFile [] fastqInputsEnd1;
    private SqwFile [] fastqInputsEnd2;
    private String [] localOutputBamFilePaths;
    private String [] localOutputBaiFilePaths;
    private String [] localOutputLogFilePaths;
    private String novocraftVersion ="V2.07.15b" ;
    private boolean manualOutput;
    
    
    @Override
    public Map<String, SqwFile> setupFiles() {
        this.runEnds = Integer.valueOf(getProperty("run_ends"));
        String [] iusAccessions = getProperty("ius_accession").split(",");
        String [] sequencerRunNames = getProperty("sequencer_run_name").split(",");
        String [] lanes = getProperty("lane").split(",");
        String [] barcodes = getProperty("barcode").split(",");
	this.queue = getOptionalProperty("queue","");         
        this.novocraftVersion = getProperty("novocraft_version");
       	manualOutput = Boolean.valueOf(getProperty("manual_output"));
	
	fastqInputsEnd1 = provisionInputFiles("fastq_inputs_end_1");
	fastqInputsEnd2 = provisionInputFiles("fastq_inputs_end_2"); 

       
       //Set up outputs
       localOutputBamFilePaths = new String[this.fastqInputsEnd1.length];
       localOutputBaiFilePaths = new String[this.fastqInputsEnd1.length];
       localOutputLogFilePaths = new String[this.fastqInputsEnd1.length];
       for (int b = 0; b < localOutputBamFilePaths.length; b++) {
       		localOutputBamFilePaths[b] = "SWID_" + iusAccessions[b] + "_" 
             	+ getProperty("rg_library") + "_" + sequencerRunNames[b] + "_" + barcodes[b] 
             	+ "_L00" + lanes[b] + "_R1_001.annotated.bam";
       		localOutputBaiFilePaths[b] = localOutputBamFilePaths[b].substring(0,localOutputBamFilePaths[b].lastIndexOf(".bam")) + ".bai";
       		localOutputLogFilePaths[b] = localOutputBamFilePaths[b].substring(0,localOutputBamFilePaths[b].lastIndexOf(".bam")) + ".log";
       }     
       
	return this.getFiles();    
    }
    
    @Override
    public void setupDirectory() {
         this.dataDir = getProperty(DATA_DIR).endsWith(File.separator) ? getProperty(DATA_DIR) : getProperty(DATA_DIR) + "/";
         this.addDirectory(getProperty(DATA_DIR));
    }
    
    
    private Job doTrim(String read1Path, String read2Path, String read1TrimmedPath, String read2TrimmedPath) {
	Job cutAdaptJob = this.getWorkflow().createBashJob("cutAdapt");
	Command command = cutAdaptJob.getCommand();

	command.addArgument(getWorkflowBaseDir() + File.separator+BIN_DIR+
                File.separator+getProperty("bundled_jre")+"/bin/java");
	command.addArgument("-Xmx500M");
	command.addArgument("-cp "+getWorkflowBaseDir() + "/classes:"+getWorkflowBaseDir() + "/lib/"+getProperty("bundled_seqware"));
	command.addArgument("net.sourceforge.seqware.pipeline.runner.PluginRunner -p net.sourceforge.seqware.pipeline.plugins.ModuleRunner -- ");
	command.addArgument("--module ca.on.oicr.pde.utilities.workflows.modules.CutAdaptModule --no-metadata -- ");
	command.addArgument("--fastq-read-1 "+read1Path+" --fastq-read-2 "+read2Path);
	command.addArgument("--output-read-1 "+read1TrimmedPath+" --output-read-2 "+read2TrimmedPath);
	command.addArgument("--cutadapt \""+getProperty("python")+ " " +getProperty("cutadapt") +"\"");
	if (!getProperty("trim_min_quality").isEmpty()) command.addArgument("--quality "+getProperty("trim_min_quality"));
	if (!getProperty("trim_min_length").isEmpty()) command.addArgument("--minimum-length "+getProperty("trim_min_length"));
	command.addArgument("--adapters-1 "+getProperty("r1_adapter_trim")+" --adapters-2 "+getProperty("r2_adapter_trim"));
	if (!getProperty("cutadapt_r1_other_params").isEmpty()) command.addArgument("--other-parameters-1 "+getProperty("cutadapt_r1_other_params"));
	if (!getProperty("cutadapt_r2_other_params").isEmpty()) command.addArgument("--other-parameters-2 "+getProperty("cutadapt_r2_other_params"));
	cutAdaptJob.setMaxMemory(getProperty("trim_mem_mb"));
	if (!this.queue.isEmpty()) {
        	cutAdaptJob.setQueue(this.queue);
        }
	return cutAdaptJob;	
    }


    @Override
    public void buildWorkflow() {
	Workflow workflow = this.getWorkflow();
        final String workflowBin=getWorkflowBaseDir() + File.separator+BIN_DIR+File.separator;
        String [] srn=getProperty("sequencer_run_name").split(",");
        String [] llanes=getProperty("lane").split(",");
        String [] lbarcodes=getProperty("barcode").split(",");
        for (int i = 0; i < this.fastqInputsEnd1.length; i++) {

		Job trimJob=null;
		String r1=this.fastqInputsEnd1[i].getProvisionedPath();
		String r2=this.fastqInputsEnd2[i].getProvisionedPath();
		String basename1 = r1.substring(r1.lastIndexOf('/')+1,r1.lastIndexOf(".fastq.gz"));
		String basename2 = r2.substring(r2.lastIndexOf('/')+1,r2.lastIndexOf(".fastq.gz"));

		if (Boolean.valueOf(getProperty("do_trim"))) {
			String trim1=basename1+".trim.fastq.gz";
			String trim2=basename2+".trim.fastq.gz";
			trimJob = doTrim(r1,r2,trim1,trim2);
			r1=trim1;
			r2=trim2;
		}

                String f1 = r1.substring(0,r1.lastIndexOf(".gz"));
                String f2 = r2.substring(0,r2.lastIndexOf(".gz"));
                
		Job confirmFastq1Job = workflow.createBashJob("confirmFastqFileRead1_"+i);
		confirmFastq1Job.getCommand().addArgument(getProperty("fastq_validator"));
		confirmFastq1Job.getCommand().addArgument("--file "+r1);
		confirmFastq1Job.getCommand().addArgument(getProperty("fastq_validator_additional_parameters"));
		if (trimJob!=null) confirmFastq1Job.addParent(trimJob);
		if (!this.queue.isEmpty()) {
			confirmFastq1Job.setQueue(this.queue);
		}

        	
		//The correct format for this is using all underscores, NOT hypens
		String runlanebarcode=srn[i]+"_"+llanes[i]+"_"+lbarcodes[i];
		String readgroup = "-o SAM $'@RG\\tID:"+runlanebarcode+"\\tPU:"+runlanebarcode+"\\tLB:"+getProperty("rg_library")+"\\tSM:"+getProperty("rg_sample_name")+"\\tPL:"+getProperty("rg_platform")+"'";

		Job jobNovo;
 		String files = "-f "+f1;
		String adapters="-a "+getProperty("r1_adapter_trim");
		String insert="";
            	if (this.runEnds == 2) {
			Job confirmFastq2Job = workflow.createBashJob("confirmFastqFileRead2_"+i);
	                confirmFastq2Job.getCommand().addArgument(getProperty("fastq_validator"));
        	        confirmFastq2Job.getCommand().addArgument("--file "+r2);
			confirmFastq2Job.getCommand().addArgument(getProperty("fastq_validator_additional_parameters"));
			if (!this.queue.isEmpty()) {
                 	       confirmFastq2Job.setQueue(this.queue);
	                }
			if (trimJob!=null) confirmFastq2Job.addParent(trimJob);
			jobNovo = workflow.createBashJob("novoalign_" + i);
			jobNovo.addParent(confirmFastq2Job);

			files+=" " +f2;
			adapters+=" " +getProperty("r2_adapter_trim");
			insert= getProperty("novoalign_expected_insert");
		}
		else {
			jobNovo = workflow.createBashJob("novoalign_" + i);
		}
	      	///Novoalign align and add read groups
	      	Command command = jobNovo.getCommand();
                command.addArgument("zcat " + r1 + " > " + f1 + ";zcat " + r2 + " > " + f2 + ";");
              	command.addArgument(workflowBin+"novocraft" + this.novocraftVersion + "/novoalign ");
		command.addArgument(insert);
                command.addArgument(files).addArgument(adapters);
		command.addArgument(getProperty("novoalign_index"));
		command.addArgument(getProperty("novoalign_input_format"));
		command.addArgument(getProperty("novoalign_threads"));
		command.addArgument(readgroup);
		command.addArgument(getOptionalProperty("novoalign_additional_parameters",""));
		command.addArgument(" 2> "+this.dataDir + localOutputLogFilePaths[i] + " | ");
		command.addArgument("perl " +workflowBin+"checkBamFile.pl | ");
		command.addArgument(workflowBin + getProperty("bundled_jre") + "/bin/java ");
                command.addArgument("-Xmx" + getProperty("picard_memory")+"M");
                command.addArgument("-jar " +  workflowBin + getProperty("picardsort"));
                command.addArgument("INPUT=/dev/stdin");
                command.addArgument("OUTPUT=" + this.dataDir + localOutputBamFilePaths[i]);
                command.addArgument("SORT_ORDER=coordinate VALIDATION_STRINGENCY=SILENT CREATE_INDEX=true");
                command.addArgument("TMP_DIR=" + getProperty("tmp_dir"));
		jobNovo.setMaxMemory(getProperty("novoalign_memory"));
		jobNovo.addParent(confirmFastq1Job);

	      	SqwFile logFile = this.createOutputFile (this.dataDir + localOutputLogFilePaths[i], "text/plain", manualOutput);
		SqwFile bamFile = this.createOutputFile(this.dataDir + localOutputBamFilePaths[i], "application/bam", manualOutput );
              	SqwFile baiFile = this.createOutputFile (this.dataDir + localOutputBaiFilePaths[i], "application/bam-index", manualOutput);
 
              	jobNovo.addFile(bamFile);
              	jobNovo.addFile(baiFile);
	      	jobNovo.addFile(logFile);
              	if (!this.queue.isEmpty()) {
                	jobNovo.setQueue(this.queue);
              	}
                          
          }
        

    }
}
