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


/*
 * Novoalign decider is compliant with the latest Hotfix (as of Jan 15, 2012) and
 * handles all new parameters that came with the latest workflow update
 * Right now identifies files for mates (paired-read sequencing) by checking names of the input files (should start using metadata in the near future)
 * This decider provides a 'nested grouping' feature that allow grouping first 
 * by then by template type (geo_library_source_template_type) and finally by group id (geo_goup_id)
 */
package ca.on.oicr.pde.deciders;

import java.util.*;
import net.sourceforge.seqware.common.module.FileMetadata;
import net.sourceforge.seqware.common.hibernate.FindAllTheFiles.Header;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.Log;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author pruzanov@oicr.on.ca
 */
public class GenomicAlignmentNovoalignDecider extends OicrDecider {

    //Patterns to search for in files' names to determine mate correctly
    private String[][] readMateFlags = {{"_R1_", "1_sequence.txt", ".1.fastq"}, {"_R2_", "2_sequence.txt", ".2.fastq"}};

    private String path = "./";
    private String folder = "seqware-results";
    private String colorspace = "0";
    private String run_ends = "2";
    //For Novoalign
    private String novoalign_r1_adapter_trim = "-a AGATCGGAAGAGCGGTTCAGCAGGAATGCCGAGACCG";
    private String novoalign_r2_adapter_trim = "AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGT";
    private String novoalign_slots = "1";
    private String novoalign_memory = "16000";
    private String novoalign_threads = "-c 8";
    private String novoalign_input_format = "";
    private String novoalign_index = "-d ${workflow_bundle_dir}/Workflow_Bundle_GenomicAlignmentNovoalign/0.10.4/data/indexes/novoalign/hg19/hg19_random/hg19_random.nix";
    private String novoalign_expected_insert = "-i PE 250,50";
    private String novoalign_additional_parameters = "-r ALL 5 -R 0 -o SAM";
    //For Picard
    private String picard_threads = "1";
    private String picard_slots = "1";
    private String picard_memory = "3000";
    private String picardmerge_slots = "1";
    //For Read Group
//    private String rg_library = "library";
//    private String rg_platform = "illumina";
//    private String rg_sample_name = "sample";
    //Hotfix addition

    private String rg_library;
    private String rg_platform;
    private String rg_sample_name;

    private String ius_accession;
    private String sequencer_run_name;
    private String lane;
    private String barcode = "NOINDEX";
    private String queue;

    public GenomicAlignmentNovoalignDecider() {
        super();
        parser.accepts("ini-file", "Optional: the location of the INI file.").withRequiredArg();
        parser.accepts("verbose", "Optional: output all SeqWare info.").withRequiredArg();
        parser.accepts("output-path", "Optional: the path where the files should be copied to after analysis. output-prefix in INI file.").withRequiredArg();
        parser.accepts("output-folder", "Optional: the folder to put the output into relative to the output-path. Corresponds to output-dir in INI file.").withRequiredArg();
        parser.accepts("template-type", "Optional: Template type for grouping samples.").withRequiredArg();
        parser.accepts("colorspace", "Optional: colorspace for Novoalign analysis, default 0.").withRequiredArg();
        parser.accepts("run-ends", "Run ends will define if it is Single-End(1) or Paired-End(2) experiment, default 2.").withRequiredArg();
        //Novoalign-specific parameters
        parser.accepts("novoalign-slots", "Optional: Novoalign slots, default 1.").withRequiredArg();
        parser.accepts("novoalign-memory", "Optional: Novoalign memory, default 16000.").withRequiredArg();
        parser.accepts("novoalign-threads", "Optional: Novoalign threads, default -c 8.").withRequiredArg();
        parser.accepts("novoalign-input-format", "Optional: Novoalign input format. No default, Novoalign will set it automatically.").withRequiredArg();
        parser.accepts("novoalign-index", "Optional: index generated with novoindex reference for reference genome, default is hg19_random.nix.").withRequiredArg();
        parser.accepts("novoalign-expected-insert", "Optional: Novoalign expected insert, default -i PE 250,50.").withRequiredArg();
        parser.accepts("novoalign-r1-adapter-trim", "Optional: Novoalign r1 adapter trim, default -a AGATCGGAAGAGCGGTTCAGCAGGAATGCCGAGACCG.").withRequiredArg();
        parser.accepts("novoalign-r2-adapter-trim", "Optional: Novoalign r2 adapter trim, default AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGT.").withRequiredArg();
        parser.accepts("novoalign-additional-parameters", "Optional: Novoalign additional parameters, default -r ALL 5 -R 0 -o SAM.").withRequiredArg();
        //Picard parameters
        parser.accepts("picard-threads", "Optional: Picard threads, default 1.").withRequiredArg();
        parser.accepts("picard-slots", "Optional: Picard slots, default 1.").withRequiredArg();
        parser.accepts("picard-memory", "Optional: Picard memory, default 3000.").withRequiredArg();
        parser.accepts("picardmerge-slots", "Optional: Picard merge slots, default 1.").withRequiredArg();
        //Read Group parameters
        parser.accepts("rg-library", "Optional: Read Group library, default library.").withRequiredArg();
        parser.accepts("rg-platform", "Optional: Sequencing platform, default illumina.").withRequiredArg();
        parser.accepts("rg-platform-unit", "Optional: Sequencing platform unit, default flowcell-barcode_lane.").withRequiredArg();
        parser.accepts("rg-sample-name", "Optional: Sample name, default sample.").withRequiredArg();
        //Hotfix addition
        parser.accepts("barcode", "Optional: Barcode, default is empty string.").withRequiredArg();
        parser.accepts("queue", "Optional: Sequencing platform, will be set to production if no value passed.").withRequiredArg();

    }

    @Override
    public ReturnValue init() {
        Log.debug("INIT");
        this.setMetaType(Arrays.asList("chemical/seq-na-fastq", "chemical/seq-na-fastq-gzip"));
        //allows anything defined on the command line to override the 'defaults' here.
        ReturnValue val = super.init();
        this.setHeader(Header.IUS_SWA);

        if (this.options.has("group-by")) {
            Log.error("Grouping by anything other than IUS_SWA does not make much sense for this workflow, ignoring...");
        }

        if (this.options.has("output-path")) {
            path = options.valueOf("output-path").toString();
            if (!path.endsWith("/")) {
                path += "/";
            }
        }
        if (this.options.has("output-folder")) {
            folder = options.valueOf("output-folder").toString();
        }

        if (this.options.has("verbose")) {
            Log.setVerbose(true);
        }
        //Parameters
        if (this.options.has("colorspace")) {
            this.colorspace = options.valueOf("colorspace").toString();
        }
        if (this.options.has("run-ends")) {
            String runEnds = options.valueOf("run-ends").toString();
            if (!runEnds.equals("2") && !runEnds.equals("1")) {
                Log.error("You passed run-ends parameter " + runEnds + ", but this decider irecognizes only 1 (single reads) and 2 (paired-reads) options");
                System.exit(1);
            }
            this.run_ends = options.valueOf("run-ends").toString();
        }
        //Novoalign-specific parameters
        if (this.options.has("novoalign-slots")) {
            this.novoalign_slots = options.valueOf("novoalign-slots").toString();
        }
        if (this.options.has("novoalign-memory")) {
            this.novoalign_memory = options.valueOf("novoalign-memory").toString();
        }
        if (this.options.has("novoalign-threads")) {
            this.novoalign_threads = options.valueOf("novoalign-threads").toString();
        }
        if (this.options.has("novoalign-input-format")) {
            this.novoalign_input_format = options.valueOf("novoalign-input-format").toString();
        }
        if (this.options.has("novoalign-index")) {
            this.novoalign_index = options.valueOf("novoalign-index").toString();
        }
        if (this.options.has("novoalign-expected-insert")) {
            String optionExpInsert = options.valueOf("novoalign-expected-insert").toString();
            if (!optionExpInsert.startsWith("-i")) {
                Log.error("Parameter novoalign-expected-insert must be of the form '-i PE 250,50' or '' (use the novoalign default).  Did you miss the -i?");
                System.exit(1);
            }
            this.novoalign_expected_insert = optionExpInsert;
        }
        if (this.options.has("novoalign-input-format")) {
            String optionInputFormat = options.valueOf("novoalign-input-format").toString();
            if (!optionInputFormat.startsWith("-F")) {
                Log.error("Parameter novoalign-input-format must be of the form \"-F ILMFQ|STDFQ|ILM1.8...\" or \"\" (to let novoalign guess for itself).  Did you miss the -F?");
                System.exit(1);
            }
            this.novoalign_input_format = optionInputFormat;
        }
        if (this.options.has("novoalign-r1-adapter-trim")) {
            String optionR1AdaptTrim = options.valueOf("novoalign-r1-adapter-trim").toString();
            if (!optionR1AdaptTrim.startsWith("-a")) {
                Log.error("Parameter novoalign-r1-adapter-trim must be of the form '-a AGATCGGA...' or '' (to turn off adapter trimming).  Did you miss the -a?");
                System.exit(1);
            }
            this.novoalign_r1_adapter_trim = options.valueOf("novoalign-r1-adapter-trim").toString();
        }
        if (this.options.has("novoalign-r2-adapter-trim")) {
            this.novoalign_r2_adapter_trim = options.valueOf("novoalign-r2-adapter-trim").toString();
        }
        if (this.options.has("novoalign-additional-parameters")) {
            this.novoalign_additional_parameters = options.valueOf("novoalign-additional-parameters").toString();
        }
        //Picard-specific parameters
        if (this.options.has("picard_threads")) {
            this.picard_threads = options.valueOf("picard_threads").toString();
        }
        if (this.options.has("picard_slots")) {
            this.picard_slots = options.valueOf("picard_slots").toString();
        }
        if (this.options.has("picard_memory")) {
            this.picard_memory = options.valueOf("picard_memory").toString();
        }
        if (this.options.has("picardmerge_slots")) {
            this.picard_memory = options.valueOf("picardmerge_slots").toString();
        }
        //Read Group parameters
        if (this.options.has("rg-library")) {
            this.rg_library = options.valueOf("rg-library").toString();
        }
        if (this.options.has("rg-platform")) {
            this.rg_platform = options.valueOf("rg-platform").toString();
        }
        if (this.options.has("rg-sample-name")) {
            this.rg_sample_name = options.valueOf("rg-sample-name").toString();
        }

        //Hotfix addition
        if (this.options.has("barcode")) {
            this.barcode = options.valueOf("barcode").toString();
        }
        if (this.options.has("queue")) {
            this.queue = options.valueOf("queue").toString();
        } else {
            this.queue = "production";
        }

        return val;
    }

    protected String handleGroupByAttribute(String attribute, String template, String group_id) {
        //group by parent name, group_id  and template type
        String[] parentNames = attribute.split(":");
        String groupBy = "";
        String[] myFilters = {parentNames[parentNames.length - 1], template, group_id};

        for (int i = 0; i < myFilters.length; i++) {
            if (null != myFilters[i]) {
                if (groupBy.length() > 1) {
                    groupBy = groupBy.concat(":" + myFilters[i]);
                } else {
                    groupBy = groupBy.concat(myFilters[i]);
                }
            }
        }
        return groupBy;
    }

    @Override
    protected boolean checkFileDetails(ReturnValue returnValue, FileMetadata fm) {
        Log.debug("CHECK FILE DETAILS:" + fm);

        if (this.options.has("template-type")) {
            if (!returnValue.getAttribute(Header.SAMPLE_TAG_PREFIX.getTitle() + "geo_library_source_template_type").equals(this.options.valueOf("template-type"))) {
                return false;
            }
        }
        //Get additional metadata
        if (null != this.ius_accession) {
            this.ius_accession = this.ius_accession + "," + returnValue.getAttribute(Header.IUS_SWA.getTitle());
        } else {
            this.ius_accession = returnValue.getAttribute(Header.IUS_SWA.getTitle());
        }

        if (null != this.sequencer_run_name) {
            this.sequencer_run_name = this.sequencer_run_name + "," + returnValue.getAttribute(Header.SEQUENCER_RUN_NAME.getTitle());
        } else {
            this.sequencer_run_name = returnValue.getAttribute(Header.SEQUENCER_RUN_NAME.getTitle());
        }

        if (null != this.lane) {
            this.lane = this.lane + "," + returnValue.getAttribute(Header.LANE_NUM.getTitle());
        } else {
            this.lane = returnValue.getAttribute(Header.LANE_NUM.getTitle());
        }

        FileAttributes rv = new FileAttributes(returnValue, returnValue.getFiles().get(0));
        String groupId = StringUtils.defaultIfBlank(rv.getLimsValue(Lims.GROUP_ID), "");
        this.rg_library = rv.getLibrarySample() + groupId;
        this.rg_platform = "illumina";
        this.rg_sample_name = rv.getDonor() + groupId;

        return super.checkFileDetails(returnValue, fm);
    }

    @Override
    public Map<String, List<ReturnValue>> separateFiles(List<ReturnValue> vals, String groupBy) {
        //get files from study
        Map<String, List<ReturnValue>> map = new HashMap<String, List<ReturnValue>>();

        //group files according to the designated header (e.g. sample SWID)
        for (ReturnValue r : vals) {
            String currVal = r.getAttributes().get(groupBy);
            String template = r.getAttribute(Header.SAMPLE_TAG_PREFIX.getTitle() + "geo_library_source_template_type");
            String group_id = r.getAttribute(Header.SAMPLE_TAG_PREFIX.getTitle() + "geo_group_id");

            currVal = handleGroupByAttribute(currVal, template, group_id);

            List<ReturnValue> vs = map.get(currVal);
            if (vs == null) {
                vs = new ArrayList<ReturnValue>();
            }
            vs.add(r);
            map.put(currVal, vs);
        }

        return map;
    }

    @Override
    protected Map<String, String> modifyIniFile(String commaSeparatedFilePaths, String commaSeparatedParentAccessions) {
        Log.debug("INI FILE:" + commaSeparatedFilePaths);
        String skipFile = "";
        //reset test mode
        if (!this.options.has("test")) {
            this.setTest(false);
        }

        Set fqInputs_end1 = new HashSet();
        Set fqInputs_end2 = new HashSet();
        Set[] fqInputFiles = {fqInputs_end1, fqInputs_end2};
        String fastq_inputs_end_1 = "";
        String fastq_inputs_end_2 = "";

        if (commaSeparatedFilePaths.contains(",")) {
            String[] fqFilesArray = commaSeparatedFilePaths.split(",");
            Set fqFilesSet = new HashSet(Arrays.asList(fqFilesArray));
            Iterator fqFiles = fqFilesSet.iterator();
            int[] indexes = {0, 1};

            while (fqFiles.hasNext()) {
                String file = fqFiles.next().toString();
                for (int i : indexes) {
                    for (int j = 0; j < readMateFlags[i].length; j++) {
                        if (file.contains(readMateFlags[i][j])) {
                            fqInputFiles[i].add(file);
                            break;
                        }
                    }
                }
            }

            if (fqInputFiles[0].size() == 0 || fqInputFiles[1].size() == 0) {
                Log.error("Was not able to retrieve fastq files for either one or two subsets of paired reads, setting mode to test");
                this.setTest(true);
            } else {
                fastq_inputs_end_1 = _join(",", fqInputFiles[0]);
                fastq_inputs_end_2 = _join(",", fqInputFiles[1]);
            }
        } else {
            this.run_ends = "1";
            fastq_inputs_end_1 = commaSeparatedFilePaths;
            fastq_inputs_end_2 = commaSeparatedFilePaths;
        }

        Map<String, String> iniFileMap = new TreeMap<String, String>();
        iniFileMap.put("fastq_inputs_end_1", fastq_inputs_end_1);
        iniFileMap.put("fastq_inputs_end_2", fastq_inputs_end_2);
        iniFileMap.put("output_prefix", this.path);
        iniFileMap.put("output_dir", this.folder);
        iniFileMap.put("colorspace", this.colorspace);
        iniFileMap.put("run_ends", this.run_ends);

        //For Novoalign
        iniFileMap.put("novoalign_r1_adapter_trim", this.novoalign_r1_adapter_trim);
        iniFileMap.put("novoalign_r2_adapter_trim", this.novoalign_r2_adapter_trim);
        iniFileMap.put("novoalign_slots", this.novoalign_slots);
        iniFileMap.put("novoalign_memory", this.novoalign_memory);
        iniFileMap.put("novoalign_threads", this.novoalign_threads);
        iniFileMap.put("novoalign_index", this.novoalign_index);
        iniFileMap.put("novoalign_input_format", this.novoalign_input_format);
        iniFileMap.put("novoalign_expected_insert", this.novoalign_expected_insert);
        iniFileMap.put("novoalign_additional_parameters", this.novoalign_additional_parameters);
        //For Picard
        iniFileMap.put("picard_threads", this.picard_threads);
        iniFileMap.put("picard_slots", this.picard_slots);
        iniFileMap.put("picard_memory", this.picard_memory);
        iniFileMap.put("picardmerge_slots", this.picardmerge_slots);
        //For Read Group
        iniFileMap.put("rg_library", this.rg_library);
        iniFileMap.put("rg_platform", this.rg_platform);
        iniFileMap.put("rg_sample_name", this.rg_sample_name);
        //Hotfix addition
        iniFileMap.put("queue", this.queue);

        if (this.run_ends.equals("2")) {
            iniFileMap.put("barcode", this.barcode + "," + this.barcode);
            iniFileMap.put("ius_accession", _getLastN(this.ius_accession, 2));
            iniFileMap.put("sequencer_run_name", _getLastN(this.sequencer_run_name, 2));
            iniFileMap.put("lane", _getLastN(this.lane, 2));
        } else {
            iniFileMap.put("barcode", this.barcode);
            iniFileMap.put("ius_accession", _getLastN(this.ius_accession, 1));
            iniFileMap.put("sequencer_run_name", _getLastN(this.sequencer_run_name, 1));
            iniFileMap.put("lane", _getLastN(this.lane, 1));
        }

        return iniFileMap;
    }

    //Join function
    public static String _join(String separator, Set items) {
        StringBuffer result = new StringBuffer();
        Iterator myItems = items.iterator();
        while (myItems.hasNext()) {
            if (result.length() > 0) {
                result.append(separator);
            }

            result.append(myItems.next().toString());
        }

        return result.toString();
    }

    //Element extractor - create array from comma-separated list and return comma-joined last n elements
    public static String _getLastN(String input, int last) {
        String[] elements = input.split(",");
        int start = elements.length - last;
        if (start < 0) {
            Log.error("Attempt to extract more elements than there are in the list " + input + " gets " + elements.length + "Elements");
            return elements[elements.length - 1]; // return just the last one
        }

        String result = elements[start];
        for (int i = start + 1; i < elements.length; i++) {
            result = result + "," + elements[i];
        }

        return result;
    }

    public static void main(String args[]) {

        List<String> params = new ArrayList<String>();
        params.add("--plugin");
        params.add(GenomicAlignmentNovoalignDecider.class.getCanonicalName());
        params.add("--");
        params.addAll(Arrays.asList(args));
        System.out.println("Parameters: " + Arrays.deepToString(params.toArray()));
        net.sourceforge.seqware.pipeline.runner.PluginRunner.main(params.toArray(new String[params.size()]));

    }

}
