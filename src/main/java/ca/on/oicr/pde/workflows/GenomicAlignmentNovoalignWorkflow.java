package ca.on.oicr.pde.workflows;

import ca.on.oicr.pde.utilities.workflows.OicrWorkflow;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.seqware.common.util.Log;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import net.sourceforge.seqware.pipeline.workflowV2.model.Workflow;
import net.sourceforge.seqware.pipeline.workflowV2.model.Command;
/**
 *
 * @author pruzanov, mtaschuk
 */
public class GenomicAlignmentNovoalignWorkflow extends OicrWorkflow {
    //private String finalOutDir;
    private String dataDir;
    private String queue;
    private int runEnds;
    private String [] fastq_inputs_end_1;
    private String [] fastq_inputs_end_2;
    private String [] iusAccessions;
    private String [] sequencerRunNames;
    private String [] lanes;
    private String [] barcodes;
    private String [] localOutputBamFilePaths;
    private String [] localOutputSamFilePaths;
    private String [] localOutputBaiFilePaths;
    private String [] localOutputLogFilePaths;
    private String novocraftVersion ="V2.07.15b" ;
    private boolean manualOutput;
    
    
    @Override
    public Map<String, SqwFile> setupFiles() {
     try {
         
         
       if (getProperty("fastq_inputs_end_1") == null) {
           Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.SEVERE, "fastq_inputs_end_1 is not set, we need at least one fastq file");
           return (null);
       } else {
           this.fastq_inputs_end_1 = getProperty("fastq_inputs_end_1").split(",");
       }
       
       if (getProperty("fastq_inputs_end_2") == null || getProperty("fastq_inputs_end_2").equals(this.fastq_inputs_end_1)) {
           Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.WARNING, "This run will treat the data as single-end reads");
           this.runEnds = 1;
       } else {
           this.fastq_inputs_end_2 = getProperty("fastq_inputs_end_2").split(",");
           this.runEnds = 2;
       }
             
       if (getProperty("ius_accession") == null) {
           Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.SEVERE, "ius_accession needs to be set!");
           return(null);
       } else {
           this.iusAccessions = getProperty("ius_accession").split(",");
       }
       
       if (getProperty("sequencer_run_name") == null) {
           Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.SEVERE, "sequencer_run_name needs to be set!");
           return(null);
       } else {
           this.sequencerRunNames = getProperty("sequencer_run_name").split(",");
       }
       
       if (getProperty("lane") == null) {
           Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.SEVERE, "lane needs to be set!");
           return(null);
       } else {
           this.lanes = getProperty("lane").split(",");
       }     
       
       if (getProperty("barcode") == null) {
           Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.SEVERE, "barcode needs to be set!");
           return(null);
       } else {
           this.barcodes = getProperty("barcode").split(",");
       }
       
       this.queue = getOptionalProperty("queue","");         
          
       
       if (getProperty("novocraft_version") == null) {
           Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.WARNING, "novocraft_version is not set, will use {0}", this.novocraftVersion);
       } else {
           this.novocraftVersion = getProperty("novocraft_version");
       }
       
       manualOutput = Boolean.valueOf(getProperty("manual_output"));
       
       //Set up inputs
       for (int fileIndex = 0;fileIndex < this.fastq_inputs_end_1.length; fileIndex++) {         
          for (int r = 1;r <= this.runEnds; r++) {
           Log.stdout("CREATING FILE: input_fastq_R" + r + "_" + fileIndex);
           SqwFile file = this.createFile("input_fastq_R" + r + "_" + fileIndex);
           switch (r){
             case 1:
              file.setSourcePath(this.fastq_inputs_end_1[fileIndex]);
             break;
             case 2:
              file.setSourcePath(this.fastq_inputs_end_2[fileIndex]);
             break;
             default:
              Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.SEVERE, "Something worng, readEnds neither 1 notr 2");
             break;
           };
           file.setType("application/bam");
           file.setIsInput(true);
          }
         }
       
       //Set up outputs
       localOutputBamFilePaths = new String[this.fastq_inputs_end_1.length];
       localOutputSamFilePaths = new String[this.fastq_inputs_end_1.length];
       localOutputBaiFilePaths = new String[this.fastq_inputs_end_1.length];
       localOutputLogFilePaths = new String[this.fastq_inputs_end_1.length];
       for (int b = 0; b < localOutputBamFilePaths.length; b++) {
       localOutputBamFilePaths[b] = "SWID_" + this.iusAccessions[b] + "_" 
             + getProperty("rg_library") + "_" + this.sequencerRunNames[b] + "_" + this.barcodes[b] 
             + "_L00" + this.lanes[b] + "_R1_001.annotated.bam";
       localOutputSamFilePaths[b] = localOutputBamFilePaths[b].substring(0,localOutputBamFilePaths[b].lastIndexOf(".bam")) + ".sam";
       localOutputBaiFilePaths[b] = localOutputBamFilePaths[b].substring(0,localOutputBamFilePaths[b].lastIndexOf(".bam")) + ".bai";
       localOutputLogFilePaths[b] = localOutputBamFilePaths[b].substring(0,localOutputBamFilePaths[b].lastIndexOf(".bam")) + ".log";
       }     
       
     } catch (Exception e) {
       Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.SEVERE, null, e);     
     }
     return this.getFiles();    
    }
    
    @Override
    public void setupDirectory() {
         this.dataDir = getProperty("data_dir").endsWith("/") ? getProperty("data_dir") : getProperty("data_dir") + "/";
         this.addDirectory(getProperty("data_dir"));
    }
    
    
    @Override
    public void buildWorkflow() {
	Workflow workflow = this.getWorkflow();
        String[] srn=getProperty("sequencer_run_name").split(","), lanes=getProperty("lane").split(","), barcodes=getProperty("barcode").split(",");
        for (int i = 0; i < this.fastq_inputs_end_1.length; i++) {
		Job confirmFastq1Job = workflow.createBashJob("confirmFastqFileRead1_"+i);
		confirmFastq1Job.getCommand().addArgument(getProperty("fastq_validator"));
		confirmFastq1Job.getCommand().addArgument("--file "+this.fastq_inputs_end_1[i]);
		confirmFastq1Job.getCommand().addArgument(getProperty("fastq_validator_additional_parameters"));
		if (!this.queue.isEmpty()) {
			confirmFastq1Job.setQueue(this.queue);
		}

        	String basename1 = this.fastq_inputs_end_1[i].substring(this.fastq_inputs_end_1[i].lastIndexOf("/")+1);
		//The correct format for this is using all underscores, NOT hypens
		String runlanebarcode=srn[i]+"_"+lanes[i]+"_"+barcodes[i];
		String readgroup = "-o SAM $'@RG\\tID:"+runlanebarcode+"\\tPU:"+runlanebarcode+"\\tLB:"+getProperty("rg_library")+"\\tSM:"+getProperty("rg_sample_name")+"\\tPL:"+getProperty("rg_platform")+"'";

		Job job_novo=null;
 		String files = "-f "+getFiles().get("input_fastq_R1_" + i).getProvisionedPath();
		String adapters=getProperty("novoalign_r1_adapter_trim");
		String insert="";
            	if (this.runEnds == 2) {
			Job confirmFastq2Job = workflow.createBashJob("confirmFastqFileRead2_"+i);
	                confirmFastq2Job.getCommand().addArgument(getProperty("fastq_validator"));
        	        confirmFastq2Job.getCommand().addArgument("--file "+this.fastq_inputs_end_2[i]);
			confirmFastq2Job.getCommand().addArgument(getProperty("fastq_validator_additional_parameters"));
			if (!this.queue.isEmpty()) {
                 	       confirmFastq2Job.setQueue(this.queue);
	                }
			job_novo = workflow.createBashJob("novoalign_" + i);
			job_novo.addParent(confirmFastq2Job);

			files+=" " +getFiles().get("input_fastq_R2_" + i).getProvisionedPath();
			adapters+=" " +getProperty("novoalign_r2_adapter_trim");
			insert= getProperty("novoalign_expected_insert");
		}
		else {
			job_novo = workflow.createBashJob("novoalign_" + i);
		}
	      	///Novoalign align and add read groups
	      	Command command = job_novo.getCommand();
              	command.addArgument(getWorkflowBaseDir() + "/bin/novocraft" + this.novocraftVersion + "/novoalign ");
		command.addArgument(insert);
                command.addArgument(files).addArgument(adapters);
		command.addArgument(getProperty("novoalign_index"));
		command.addArgument(getProperty("novoalign_input_format"));
		command.addArgument(getProperty("novoalign_threads"));
		command.addArgument(readgroup);
		command.addArgument(getOptionalProperty("novoalign_additional_parameters",""));
		command.addArgument(" 2> "+this.dataDir + localOutputLogFilePaths[i] + " | ");
		command.addArgument("perl " +getWorkflowBaseDir() + "/bin/checkBamFile.pl | ");
		command.addArgument(getWorkflowBaseDir() + "/bin/" + getProperty("bundled_jre") + "/bin/java ");
                command.addArgument("-Xmx" + getProperty("picard_memory")+"M");
                command.addArgument("-jar " +  getWorkflowBaseDir() + "/bin/" + getProperty("picardsort"));
                command.addArgument("INPUT=/dev/stdin");
                command.addArgument("OUTPUT=" + this.dataDir + localOutputBamFilePaths[i]);
                command.addArgument("SORT_ORDER=coordinate VALIDATION_STRINGENCY=SILENT CREATE_INDEX=true");
                command.addArgument("TMP_DIR=" + getProperty("tmp_dir"));
		job_novo.setMaxMemory(getProperty("novoalign_memory"));
		job_novo.addParent(confirmFastq1Job);

	      	SqwFile log_file = this.createOutputFile (this.dataDir + localOutputLogFilePaths[i], "text/plain", manualOutput);
		SqwFile bam_file = this.createOutputFile(this.dataDir + localOutputBamFilePaths[i], "application/bam", manualOutput );
              	SqwFile bai_file = this.createOutputFile (this.dataDir + localOutputBaiFilePaths[i], "application/bam-index", manualOutput);
 
              	job_novo.addFile(bam_file);
              	job_novo.addFile(bai_file);
	      	job_novo.addFile(log_file);
              	if (!this.queue.isEmpty()) {
                	job_novo.setQueue(this.queue);
              	}
                          
	      	///Sort SAM, create the bam and bam index
		//Job jobBam = workflow.createBashJob("PicardSortSamMakeBam_"+i);
//		command = jobBam.getCommand();
//	      	command.addArgument(getWorkflowBaseDir() + "/bin/" + getProperty("bundled_jre") + "/bin/java ");
//		command.addArgument("-Xmx" + getProperty("picard_memory")+"M");
//		command.addArgument("-jar " +  getWorkflowBaseDir() + "/bin/" + getProperty("picardsort"));
//		command.addArgument("INPUT="+this.dataDir + localOutputSamFilePaths[i]);
//		command.addArgument("OUTPUT=" + this.dataDir + localOutputBamFilePaths[i]);
//		command.addArgument("SORT_ORDER=coordinate VALIDATION_STRINGENCY=SILENT CREATE_INDEX=true");
//		command.addArgument("TMP_DIR=" + getProperty("tmp_dir"));
//	      	jobBam.addParent(job_novo);
//	      	jobBam.setMaxMemory((Integer.valueOf(getProperty("picard_memory")) *2)+"");
          }
        

    }
}
