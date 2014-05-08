#!/usr/bin/env perl

use strict;
use warnings;

my $is_empty=1;

while (<STDIN>) {
	print;
	my $line=$_;
	$line =~ s/^\s+|\s+$//g;
	if (length $line and $line !~ /^@/ ){
		$is_empty=0;
		#iterate through the rest
		while (<STDIN>){
		    print;
		}
	}
}

die "empty SAM file" if $is_empty;
