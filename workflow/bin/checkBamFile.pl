#!/usr/bin/env perl

###
#  Copyright (C) 2014  Ontario Institute of Cancer Research
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
# Contact us:
# 
#  Ontario Institute for Cancer Research  
#  MaRS Centre, West Tower
#  661 University Avenue, Suite 510
#  Toronto, Ontario, Canada M5G 0A3
#  Phone: 416-977-7599
#  Toll-free: 1-866-678-6427
#  www.oicr.on.ca
###


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
