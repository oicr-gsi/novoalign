use strict;
use DBI;
use Data::Dumper;
use Getopt::Long;

# Author: briandoconnor@gmail.com
# Desc:   A simple decider that finds all the IUS that correspond to human samples and runs the Novoalign whole genome alignment workflow on them.
#         It can operate on a flowcell/lane level or on a sample basis.  In each case it looks for candidate IUS that are human, experimental design
#         is whole genome or whole exome sequencing, and this workflow hasn't previously been run on the IUS.
# 
# Hotfix: exposed a number of novoalign parameters and merged with Zheng's custom decider  -Rob
# Hotfix: added group ID functionality and skip_casava_check  -Rob
#
# TODO:
# * 


my ($barcode, $username, $password, $outputPath, $dbhost, $seqware_meta_db, $flowcell, $wf_accession, $max_flowcells, $lane_to_process, $after_date, $skip_workflow_run_check, $test, $parent_wf_accession, $org, $organism, $debug, $skip_parent_workflow_run_status_check, $novoalign_expected_insert, $novoalign_r1_adapter_trim, $novoalign_r2_adapter_trim, $novoalign_input_format, $novoalign_additional_parameters);

# /.mounts/labs/PDE/public/provisioned-bundles/sqwstage/Workflow_Bundle_GenomicAlignmentNovoalign/
# /u/seqware/provisioned-bundles/sqwprod/Workflow_Bundle_GenomicAlignmentNovoalign_0.10.3_SeqWare_0.10.0/
# /u/seqwaretest/provisioned-bundles/sqwstage/Workflow_Bundle_GenomicAlignmentNovoalign/Workflow_Bundle_GenomicAlignmentNovoalign/0.10.3/lib/seqware-pipeline-0.10.0.jar
# /u/seqwaretest/provisioned-bundles/sqwstage/
my $seqware_bundle_dir = "/.mounts/labs/seqware/public/provisioned-bundles/sqwprod/";

# hot fix default parameters
$novoalign_expected_insert = "-i PE 250,50";
$novoalign_r1_adapter_trim = "-a AGATCGGAAGAGCGGTTCAGCAGGAATGCCGAGACCG";
$novoalign_r2_adapter_trim = "AGATCGGAAGAGCGTCGTGTAGGGAAAGAGTGT";

$novoalign_input_format = "";
$novoalign_additional_parameters = "-r ALL 5 -R 0 -o SAM";

my $lsOutput;

my $c = getConfig();
$max_flowcells = -1;
my $max_flow_hash = {};
$username = $c->{SW_DB_USER};
$password = $c->{SW_DB_PASS};
$dbhost = $c->{SW_DB_SERVER};
$seqware_meta_db = $c->{SW_DB};
$outputPath = "/oicr/data/archive/seqware/seqware_analysis/";
$skip_workflow_run_check = 0;
$test = 0;
$skip_parent_workflow_run_status_check = 0;
#default
$org = 'Homo sapiens';

# some variables used in sample name creation
# samples are currently named (in the read_group sample field) as CPCG_0003_Pr_P
# first two elements are sample ID, then tissue origin (Pr=prostate), then tissue type (P=primary tumor)
# geo_tissue_origin
my $tissue_origin = "";
# geo_tissue_type
my $tissue_source = "";

my $argSize = scalar(@ARGV);

my $getOptResult = GetOptions('barcode=s' => \$barcode, 'username=s' => \$username, 'password=s' => \$password, 'outputPath=s' => \$outputPath, 'dbhost=s' => \$dbhost, 'seqware-meta-db=s' => \$seqware_meta_db, 'flowcell=s' => \$flowcell, 'wf-accession=i' => \$wf_accession, 'parent-wf-accession=s' => \$parent_wf_accession, 'max-flowcells=i' => \$max_flowcells, 'lane=i' => \$lane_to_process, 'after-date=s' => \$after_date, 'test' => \$test, 'debug' => \$debug, 'skip_workflow_run_check' => \$skip_workflow_run_check, 'skip_parent_workflow_run_status_check' => \$skip_parent_workflow_run_status_check, 'novoalign-expected-insert=s' => \$novoalign_expected_insert, 'novoalign-r1-adapter-trim=s' => \$novoalign_r1_adapter_trim, 'novoalign-r2-adapter-trim=s' => \$novoalign_r2_adapter_trim, 'novoalign-input-format=s' => \$novoalign_input_format, 'novoalign-additional-parameters=s' => \$novoalign_additional_parameters);
usage() if ( $argSize < 4 || !$getOptResult);

#my %org_map = ('human' => 'Homo sapiens', 'mouse' => 'Mus musculus');
#if (defined($organism)) {
#    if ( scalar grep {lc($organism) eq $_} keys %org_map != 1 ) {
#	print "$organism not recognizable\n";
#	print "permitted organisms are ", join(" ", keys %org_map), "\n";
#	exit 2;
#    }
#    $org = $org_map{$organism};
#}

# check expected insert and r1 adapter are correctly formed
if ($novoalign_expected_insert ne "")
{
    unless ($novoalign_expected_insert =~ /^-i.*/)
    {
        die "novoalign-expected-insert must be of the form \"-i PE 250,50\" or \"\" (use the novoalign default).  Did you miss the -i?\n";
    }
}

if ($novoalign_r1_adapter_trim ne "")
{
    unless ($novoalign_r1_adapter_trim =~ /^-a.*/)
    {
        die "novoalign-r1-adapter-trim must be of the form \"-a AGATCGGA...\" or \"\" (to turn off adapter trimming).  Did you miss the -a?\n";
    }
}

if ($novoalign_input_format ne "")
{
	unless ($novoalign_input_format =~ /^-F.*/)
	{
		die "novoalign-input-format must be of the form \"-F ILMFQ|STDFQ|ILM1.8...\" or \"\" (to let novoalign guess for itself).  Did you miss the -F?\n";
	}
}

my %org_idx = ('Homo sapiens' => '/oicr/data/genomes/homo_sapiens_mc/UCSC/hg19_random/Genomic/novocraft/hg19_random.nix',
	       'Mus musculus' => '/oicr/data/genomes/mus_musculus/UCSC/Genomic/mus_musculus_random/novocraft/2.07.14/mm9.nix');
my %org_ref = ('Homo sapiens' => '/.mounts/labs/seqware/public/seqware/datastore/bundles/data/indexes_novoalign_hg19_random-20110807.zip',
	       'Mus musculus' => '/oicr/data/genomes/mus_musculus/UCSC/Genomic/mus_musculus_random/novocraft/2.07.14/mm9.fa');
my $novoalign_index = $org_idx{$org};
my $ref_bundle=$org_ref{$org};

#print "##org is $org\n";
# check to see if running already
if (-e "/tmp/.GenomicAlignmentNovoalign.pl.running") {
  print "/tmp/.GenomicAlignmentNovoalign.pl.running exists so exiting!\n";
  exit 0;
}
system("touch /tmp/.GenomicAlignmentNovoalign.pl.running");

my $parent_wf_accession_string = "( $parent_wf_accession )";
my @parent_wf_accession_list = split /,/, $parent_wf_accession;

# for DBI
my $dsn = "DBI:Pg:dbname=$seqware_meta_db;host=$dbhost";

my $database=DBI->connect($dsn, $username, $password, {RaiseError => 1});

my $query = " select i.ius_id, i.tag, s.title, sr.name, sr.file_path, l.name, l.lane_index+1 as lane, 
                   (select count(*) from experiment_spot_design_read_spec where experiment_spot_design_id = e.experiment_spot_design_id and read_class = 'Application Read') as num_reads,
                   wr.workflow_run_id, i.sw_accession, s.sample_id, o.name, w.name, w.sw_accession
              from ius as i, sample as s, sequencer_run as sr, experiment as e, 
                   experiment_library_design as eld, library_strategy as ls, organism as o, lane as l,
                   ius_workflow_runs as iwr, workflow_run as wr, workflow as w
              where "; 
if ($skip_parent_workflow_run_status_check) {
$query .= "
              i.ius_id in
                    (select iwr.ius_id from ius_workflow_runs as iwr, workflow_run as wr, workflow as w where iwr.workflow_run_id = wr.workflow_run_id and
                    wr.workflow_id = w.workflow_id and w.sw_accession in $parent_wf_accession_string)
                    and ";
}
else {
$query .= "
              i.ius_id in 
                    (select iwr.ius_id from ius_workflow_runs as iwr, workflow_run as wr, workflow as w where iwr.workflow_run_id = wr.workflow_run_id and
                    wr.workflow_id = w.workflow_id and w.sw_accession in $parent_wf_accession_string and wr.status = 'completed')
                    and ";
}
unless ($skip_workflow_run_check) {
 $query .= "  i.ius_id not in 
                    (select iwr.ius_id from ius_workflow_runs as iwr, workflow_run as wr, workflow as w where iwr.workflow_run_id = wr.workflow_run_id and
                    wr.workflow_id = w.workflow_id and w.sw_accession = $wf_accession) 
                    and "; 
}

$query .= "                 i.lane_id = l.lane_id
                    and sr.sequencer_run_id = l.sequencer_run_id
                    and i.sample_id = s.sample_id and s.experiment_id = e.experiment_id and e.experiment_library_design_id = eld.experiment_library_design_id and eld.strategy = ls.library_strategy_id 
                    and (ls.name = 'WGS' or ls.name = 'WXS') 
                    and s.organism_id = o.organism_id
                    and i.ius_id = iwr.ius_id 
                    and iwr.workflow_run_id = wr.workflow_run_id
                    and wr.workflow_id = w.workflow_id
				    and i.skip is NOT TRUE
                   ";


                   
# check if we specified flowcell
if (defined($flowcell) && $flowcell ne "") {
  $query = $query." and sr.name = '$flowcell' and sr.skip is NOT TRUE";
}

# check if we specified barcode
if (defined($barcode) && $barcode ne "") {
  $query = $query." and i.tag = '$barcode'";
}

# check if we specified lane
if (defined($lane_to_process) && $lane_to_process ne "") {
  my $lane_index = $lane_to_process - 1;
  $query = $query." and l.lane_index = $lane_index and l.skip is NOT TRUE";
}

# check is a date was specified
if (defined($after_date) && $after_date ne "") {
  $query = $query." and sr.create_tstmp > '".$after_date."'";
}       

# query to pull back the file records
my $query2 = "select f.file_path, f.meta_type, p.sw_accession from ius as i, processing_ius as pi, processing as p, processing_files as pf, file as f, 
                      workflow_run as wr 
               where i.ius_id = ? and i.ius_id = pi.ius_id and pi.processing_id = p.processing_id and (p.ancestor_workflow_run_id = wr.workflow_run_id or p.workflow_run_id = wr.workflow_run_id) 
                     and wr.workflow_run_id = ? and pf.processing_id = p.processing_id and pf.file_id = f.file_id and p.algorithm in ('BCLToFastq', 'fileImport') 
                     and f.meta_type = 'chemical/seq-na-fastq-gzip' 
               order by file_path";
               
my $sth2 = $database->prepare($query2);


# query to find parent sample name
my $query4 = "select sh.sample_id, sh.parent_id, s2.name, s.name from sample_hierarchy as sh, sample as s, sample as s2 where sh.sample_id = ? and s2.sample_id = sh.sample_id and s.sample_id = sh.parent_id";
my $sth4 = $database->prepare($query4);

# query to find tissue
my $query5 = "select tag, value from sample_attribute where sample_id = ? and tag = ?";
my $sth5 = $database->prepare($query5);

# query to pull back version string for workflow
my $wf_version = "0.9.0";
my $query3 = "select version from workflow where sw_accession = $wf_accession";
my $sth3 = $database->prepare($query3);
$sth3->execute();
if (my @row = $sth3->fetchrow_array) {
  $wf_version = $row[0];
}
if ($debug) {print "wf_version is $wf_version\n";}
$sth3->finish();


# query to recursively search up the sample hierarchy for a group id
my $getSampleParentQuery = "SELECT sh.parent_id FROM sample_hierarchy AS sh WHERE sh.sample_id = ?";
my $getGroupIDQuery = "SELECT sa.value FROM sample_attribute AS sa WHERE sa.sample_id = ? AND sa.tag = 'geo_group_id'";

my $getSampleParentHandle = $database->prepare($getSampleParentQuery);
my $getGroupIDHandle = $database->prepare($getGroupIDQuery);

# now iterate over the query results

my $flowcells = {};
my $index = 0;

my $sth = $database->prepare($query);
$sth->execute();
my $count = 0;
  while(my @row = $sth->fetchrow_array) {
  	
  	$count++;

        # reset these
	# geo_tissue_origin
	$tissue_origin = "";
	# geo_tissue_type
	$tissue_source = "";
  	
  	my $ius_id = $row[0];
  	my $tag = $row[1];
  	my $library = $row[2];
  	my $sr = $row[3];
        $max_flow_hash->{$sr} = 1;

        if (scalar(keys %{$max_flow_hash}) <= $max_flowcells || $max_flowcells < 0) {

  	my $sr_path = $row[4];
  	my $lane = $row[5];
  	my $lane_num = $row[6];
  	my $read_ends = $row[7];
  	my $wr_id = $row[8];
  	my $ius_accession = $row[9];
  	my $sample_id = $row[10];
	my $organism = $row[11];
	my $this_wf = $row[12];
	my $this_wf_accession = $row[13]; 
        # constructing the sample name for the readgroup
        # this find_sample_name() method actually will find the tissue_origin and tissue_source along the way
        my $sample = find_sample_name($sample_id);
		my $groupID = findGroupID($sample_id);
		if ($groupID ne "")
		{
			$groupID = "_$groupID";
		}

	print "use in SM field $sample\n";
        if ($tissue_origin ne "") {
           $sample = $sample."_".$tissue_origin;
        }
        if ($tissue_source ne "") {
           $sample = $sample."_".$tissue_source;
        }
	print "SM is $sample\n";
  	if (!defined($tag) || $tag eq '') { $tag = "NoIndex"; }

    print "SUMMARY:\nsequencer_run_path\tsequencer_run\tread_ends\tlane\tbarcode\tsample\tlibrary\twr_id\tworkflow\tworkflow_sw_accession\n$sr_path\t$sr\t$read_ends\t$lane_num\t$tag\t$sample\t$organism\t$library\t$wr_id\t$this_wf\t$this_wf_accession\n";

	my $novoalign_index;
	my $ref_bundle;
	eval {
	    $novoalign_index = $org_idx{$organism};
	    $ref_bundle = $org_ref{$organism};
	};
	die "can not find index file for $organism\n" if ($@);

    my $rand = substr(rand(), 3);
    
    open OUT, ">/tmp/GenomicAlignmentNovoalign_$rand.ini";
    my $ini_txt = "bundledir=/.mounts/labs/seqware/public/seqware/datastore/provisioned
tmp_dir=tmp
java=/.mounts/labs/seqware/public/java/default/bin/java
output_dir=results
output_prefix=$outputPath
colorspace=0
run_ends=$read_ends
novoalign_index=-d $novoalign_index
ref_bundle=$ref_bundle
novoalign_slots=1
novoalign_threads=-c 8
novoalign_memory=24000
novoalign_input_format=$novoalign_input_format
novoalign_expected_insert=$novoalign_expected_insert
novoalign_r1_adapter_trim=$novoalign_r1_adapter_trim
novoalign_r2_adapter_trim=$novoalign_r2_adapter_trim
novoalign_additional_parameters=$novoalign_additional_parameters
picardrg=picard-tools-1.48/AddOrReplaceReadGroups.jar
picardconvert=picard-tools-1.48/SamFormatConverter.jar
picard_slots=1
picard_threads=1
picard_memory=5000
picardsort=picard-tools-1.48/SortSam.jar
picardmerge=picard-tools-1.48/MergeSamFiles.jar
picardmerge_slots=1
rg_library=$library$groupID
rg_platform=illumina
rg_platform_unit=$sr\-$tag\_$lane_num
rg_sample_name=$sample$groupID
ius_accession=$ius_accession
sequencer_run_name=$sr
lane=$lane_num
barcode=$tag
";

    # find the fastq files associated with this IUS and workflow_run
    $sth2->execute($ius_id, $wr_id);
    my $row_count = 0;
    my $parent_accession = 0;
    while(my @row = $sth2->fetchrow_array) {
    	$row_count++;
    	my $file_path = $row[0];
    	$parent_accession = $row[2];
    	$ini_txt .= "fastq_inputs_end_$row_count=$file_path\n";

		# so Zheng can see the file sizes
		$lsOutput = `ls -lh $file_path`;
		print $lsOutput;

	if ($debug) {print "fastq_inputs_end_$row_count=$file_path\n";}
    }
    # the parent accession
    $ini_txt .= "parent_accession=$parent_accession\n";

	if (scalar grep {$this_wf_accession == $_} @parent_wf_accession_list) {
    print OUT $ini_txt;
    close OUT;
    print "\nINI:\n$ini_txt\n";

    if ($row_count != $read_ends) { 
    	system("rm /tmp/.GenomicAlignmentNovoalign.pl.running");
    	die "ERROR: the number of fastq files in the DB ($row_count) must equal the number of reads ends ($read_ends)\n";
    }
    print "##zheng, you need to run this one. SUMMARY:\nsequencer_run_path\tsequencer_run\tread_ends\tlane\tbarcode\tsample\tlibrary\twr_id\tworkflow\tworkflow_sw_accession\n$sr_path\t$sr\t$read_ends\t$lane_num\t$tag\t$sample\t$organism\t$library\t$wr_id\t$this_wf\t$this_wf_accession\n";	
#	my $command_txt = "java -jar /u/seqware/provisioned-bundles/sqwprod/Workflow_Bundle_GenomicAlignmentNovoalign_0.10.1_SeqWare_0.10.0/Workflow_Bundle_GenomicAlignmentNovoalign/0.10.1/lib/seqware-pipeline-0.10.0.jar --plugin net.sourceforge.seqware.pipeline.plugins.WorkflowLauncher -- --workflow-accession $wf_accession --link-workflow-run-to-parents $ius_accession --ini-files /tmp/GenomicAlignmentNovoalign_$rand.ini";
	 my $command_txt = "java -jar ${seqware_bundle_dir}/Workflow_Bundle_GenomicAlignmentNovoalign_0.10.4_SeqWare_0.13.5/Workflow_Bundle_GenomicAlignmentNovoalign/0.10.4/lib/seqware-distribution-0.13.5-full.jar --plugin net.sourceforge.seqware.pipeline.plugins.WorkflowLauncher -- --workflow-accession $wf_accession --link-workflow-run-to-parents $ius_accession --ini-files /tmp/GenomicAlignmentNovoalign_$rand.ini";
    #my $command_txt = "java -jar /u/seqware/provisioned-bundles/sqwprod/bundle_GenomicAlignment_Novoalign_0.9.2/bundle_GenomicAlignment_Novoalign/0.9.2/lib/seqware-pipeline-0.10.0.jar --plugin net.sourceforge.seqware.pipeline.plugins.WorkflowLauncher -- --workflow-accession $wf_accession --link-workflow-run-to-parents $ius_accession --ini-files /tmp/GenomicAlignmentNovoalign_$rand.ini";

    # old decider style
    #my $command_txt = "./bin/pegasus-run.pl --workflow-accession=$wf_accession --link-parent-ius-to-workflow-run=$ius_accession /tmp/GenomicAlignmentNovoalign_$rand.ini workflows/GenomicAlignment/Novoalign/GenomicAlignmentNovoalign_$wf_version.ftl"; 
    print "COMMAND:\n$command_txt\n\n";
#    if (!$test) { system $command_txt; }
    }
} #end if $this_wf_accession == $wf_accession
	else {
	    print "WARNING: db has record of parent workflow run with sw_accession other than that given in command.\n";
	    
	}
  }
$sth->finish();
$sth2->finish();

if ($count == 0) { print "NO MATCHES IN THE DB\n"; }

# close connection
$sth4->finish();
$sth5->finish();
$database->disconnect();

# cleanup touch file
system("rm /tmp/.GenomicAlignmentNovoalign.pl.running");

sub usage
{
	print "Unknown option: @_\n" if ( @_ );
	print "usage: program [--username USERNAME] [--password PASSWORD] [--outputPath OUTPUTPATH] [--dbhost DBHOST] [--seqware-meta-db SEQWARE_META_DB] [--novoalign-expected-insert \"-i MODE MEAN,STDEV\" or \"\"] [--novoalign-r1-adapter-trim \"-a READ_1_ADAPTER\" or \"\"] [--novoalign-r2-adapter-trim \"READ_2_ADAPTER\" or \"\"] [--novoalign-input-format \"\" or \"-F STDFQ|ILMFQ|ILM1.8\"] [--novoalign-additional-parameters \"-r ALL 5 -R 0 -o SAM ...\"] [--flowcell FLOWCELL_NAME] [--wf-accession ACCESSION] [--parent-wf-accession ACCESSION] [--lane LANE_NUM] [--after-date SQL_DATE_STRING] [--barcode BARCODE_NUC] [[--help|-?] [--organism ORGANISM_NAME]\n";
	exit 2;
}

# Read the cluster id from the seqware settings file
sub getConfig {
  # attempt to read from the .seqware/settings file
  my $config_hash = {};
  my $config_file = $ENV{"HOME"} . "/.seqware/settings";
  if (defined $ENV{'SEQWARE_SETTINGS'}) {
    $config_file = $ENV{'SEQWARE_SETTINGS'};
  }
  print "Attempting to use config file: [$config_file]\n";
  if (-e $config_file) {
      open(CFG, '<', "$config_file") or die "Unable to open config file $!\n";
      while(<CFG>) {
          chomp;
          my @t = split /=/;
          $config_hash->{$t[0]} = $t[1];
      }
      close CFG;
  } else {
    print "Unable to locate config file.\n";
  }

  return($config_hash);

}

sub find_sample_name {
  my $sample_id = shift;

  my ($s_id, $parent_id, $sample_name, $parent_name);

  $sth4->execute($sample_id);
  if(my @row = $sth4->fetchrow_array) {
    $s_id = $row[0];
    $parent_id = $row[1];
    $sample_name = $row[2];
    $parent_name = $row[3]; 

    # find the associated  tissue info
    my $curr_tissue_origin = find_tissue_origin($s_id);
    my $curr_tissue_source = find_tissue_source($s_id);
    if ($curr_tissue_origin ne "" && $tissue_origin eq "") {
       $tissue_origin = $curr_tissue_origin;
    }
    if ($curr_tissue_source ne "" && $tissue_source eq "") {
       $tissue_source = $curr_tissue_source;
    }
    print "FOUND ONE $sample_name $s_id\n";
    my $new_name = find_sample_name($parent_id);
    if ($new_name eq "") { return $parent_name; }
    else { return $new_name; }
  } else {
    return("");
  }

}

sub findGroupID
{
	my $sampleID = $_[0];
	my ($parentID, $groupID);
	my $temp;

	$getGroupIDHandle->execute($sampleID);
	if (($temp) = $getGroupIDHandle->fetchrow_array)
	{
		$groupID = $temp;
		# we assume there is only one group id - this checks for a second!
		if (($temp) = $getGroupIDHandle->fetchrow_array)
		{
			warn "\n*****\nERROR: sample_attributes has multiple geo_group_ids!  May not have found the correct group ID!\n*****\n\n";
		}
		return $groupID;
	}
	else
	{
		$getSampleParentHandle->execute($sampleID);
		if (($temp) = $getSampleParentHandle->fetchrow_array)
		{
			$parentID = $temp;
			# assuming there is only one row!
			if (($temp) = $getSampleParentHandle->fetchrow_array)
			{
				warn "\n*****\nERROR: Sample has multiple parents in sample_hierarchy!  May not have found the correct group ID!\n*****\n\n";
			}
			return findGroupID($parentID);
		}
		else
		{
			return "";
		}
	}

}

sub find_tissue_origin() {
  my ($s_id) = @_;
  $sth5->execute($s_id, "geo_tissue_origin");
  if (my @row = $sth5->fetchrow_array) {
    return($row[1]);
  }
  return("");
}

sub find_tissue_source() {
  my ($s_id) = @_;
  $sth5->execute($s_id, "geo_tissue_type");
  if (my @row = $sth5->fetchrow_array) {
    return($row[1]);
  }
  return("");
}

