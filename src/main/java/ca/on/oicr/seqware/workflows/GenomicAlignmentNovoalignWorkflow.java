package ca.on.oicr.seqware.workflows;

import ca.on.oicr.pde.utilities.workflows.OicrWorkflow;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.seqware.common.util.Log;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import net.sourceforge.seqware.pipeline.workflowV2.model.Workflow;

/**
 *
 * @author pruzanov
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
       
       if (getProperty("queue") == null) {
            Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.WARNING, "Queue not set, most likely will run as default queue");
            this.queue = "";
            return (null);
        } else {
            this.queue = getProperty("queue");         
        }
          
       
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
   try {
//          String outdir = null;
//         if (getProperty("output_dir") != null && getProperty("output_prefix") != null) {
//          outdir = getProperty("output_prefix") + getProperty("output_dir")
//                             + "/seqware-" + this.getSeqware_version() + "_" + this.getName() + "_" + this.getVersion() + "/" + this.getRandom() + "/";
//         }
//        
//         outdir = (null != outdir) ? outdir : "seqware_results";
//         this.addDirectory(outdir);
//         this.finalOutDir = outdir;
         this.dataDir = getProperty("data_dir").endsWith("/") ? getProperty("data_dir") : getProperty("data_dir") + "/";
         this.addDirectory(getProperty("data_dir"));
                 
       } catch (Exception e) {
         Logger.getLogger(GenomicAlignmentNovoalignWorkflow.class.getName()).log(Level.WARNING, null, e);
       }
    }
    
    
    @Override
    public void buildWorkflow() {
        try{
        Workflow workflow = this.getWorkflow();
                 
          for (int i = 0; i < this.fastq_inputs_end_1.length; i++) {
              String basename1 = this.fastq_inputs_end_1[i].substring(this.fastq_inputs_end_1[i].lastIndexOf("/")+1);
              Job job_novo = workflow.createBashJob("novoalign_" + i);
              job_novo.setCommand(getWorkflowBaseDir() + "/bin/novocraft" + this.novocraftVersion + "/novoalign ");
             if (this.runEnds == 2) {
                job_novo.getCommand().addArgument(
                           "-f " + getFiles().get("input_fastq_R1_" + i).getProvisionedPath() + " " 
                                 + getFiles().get("input_fastq_R2_" + i).getProvisionedPath() + " "
                         + getProperty("novoalign_expected_insert") + " " 
                         + getProperty("novoalign_r1_adapter_trim") + " "
                         + getProperty("novoalign_r2_adapter_trim"));
              } else {
                job_novo.getCommand().addArgument(
                               "-f " + getFiles().get("input_fastq_R1_" + i).getProvisionedPath() + " "
                             + getProperty("novoalign_r1_adapter_trim"));
              }
                job_novo.getCommand().addArgument( 
                          getProperty("novoalign_index") + " "
                        + getProperty("novoalign_input_format") + " " 
                        + getProperty("novoalign_threads") + " " 
                        + getProperty("novoalign_additional_parameters") 
			+ " 2> "+this.dataDir + localOutputLogFilePaths[i] + " > " +this.dataDir + localOutputSamFilePaths[i]);


              job_novo.setMaxMemory(getProperty("novoalign_memory"));
	      SqwFile log_file = this.createOutputFile (this.dataDir + localOutputLogFilePaths[i], "text/plain", manualOutput);
	      job_novo.addFile(log_file);
              if (!this.queue.isEmpty()) {
                job_novo.setQueue(this.queue);
              }
                          
	      Job jobBam = workflow.createBashJob("PicardSamToBam_"+i);
	      jobBam.setCommand(getWorkflowBaseDir() + "/bin/" + getProperty("bundled_jre") + "/bin/java "
                        + "-Xmx" + getProperty("picard_memory") + "M -jar "
                        +  getWorkflowBaseDir() + "/bin/" + getProperty("picardsort") + " "
                        + "INPUT="+this.dataDir + localOutputSamFilePaths[i] + " "
                        + "OUTPUT=" + this.dataDir + basename1 + ".sorted.bam "
                        + "SORT_ORDER=coordinate "
                        + "VALIDATION_STRINGENCY=SILENT CREATE_INDEX=true TMP_DIR=" + getProperty("tmp_dir"));
	      jobBam.addParent(job_novo);
	      jobBam.setMaxMemory((Integer.valueOf(getProperty("picard_memory")) *2)+"");
 
              Job job_paddrg = workflow.createBashJob("PicardAddReadGroups_" + i);
              String wra = null == this.getWorkflow_run_accession() ? "testing" : this.getWorkflow_run_accession().toString();
              job_paddrg.setCommand(
                          getWorkflowBaseDir() + "/bin/" + getProperty("bundled_jre") + "/bin/java "
                        + "-Xmx" + getProperty("picard_memory") + "M -jar "
                        +  getWorkflowBaseDir() + "/bin/" + getProperty("picardrg") + " "
                        + "INPUT=" + this.dataDir + basename1 + ".sorted.bam "
                        + "OUTPUT=" + this.dataDir + localOutputBamFilePaths[i] + " "
                        + "SORT_ORDER=coordinate "
                        + "VALIDATION_STRINGENCY=SILENT TMP_DIR=" + getProperty("tmp_dir") + " "
                        + "RGID=SWID:" + wra + ":" + i + " "
                        + "RGLB=" + getProperty("rg_library") + " "
                        + "RGPL=" + getProperty("rg_platform") + " "
                        + "RGPU=" + getProperty("rg_platform_unit") + " "
                        + "RGSM=" + getProperty("rg_sample_name") + " "
                        + "CREATE_INDEX=true");      
              //SqwFile bam_file = this.createOutFile("application/bam",
              //                                       this.dataDir + localOutputBamFilePaths[i],
              //                                       this.finalOutDir + localOutputBamFilePaths[i],
              //                                       true);
              
              SqwFile bam_file = this.createOutputFile(this.dataDir + localOutputBamFilePaths[i], "application/bam", manualOutput );
              //SqwFile bai_file = this.createOutFile("application/bam-index",
              //                                      this.dataDir + localOutputBaiFilePaths[i],
              //                                       this.finalOutDir + localOutputBaiFilePaths[i],
              //                                      true);
              
              SqwFile bai_file = this.createOutputFile (this.dataDir + localOutputBaiFilePaths[i], "application/bam-index", manualOutput);
 
              job_paddrg.addFile(bam_file);
              job_paddrg.addFile(bai_file);
              job_paddrg.addParent(jobBam);
              job_paddrg.setMaxMemory((Integer.valueOf(getProperty("picard_memory")) *2)+"");
              if (!this.queue.isEmpty()) {
                job_paddrg.setQueue(this.queue);
              }
              
          }
          
        } catch (Exception e) {
          Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }

    }
    
//    private SqwFile createOutFile (String meta,String source,String outpath,boolean force) {
//        SqwFile file = new SqwFile();
//        file.setType(meta);
//        file.setSourcePath(source);
//        file.setIsOutput(true);
//        file.setOutputPath(outpath);
//        file.setForceCopy(force);
//        
//        return file;
//    }
    
}
