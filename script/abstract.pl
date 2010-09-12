#!/usr/bin/perl
use strict;
use warnings;

my $c = 0;
while (my $line = <>) {
    chomp $line;
    if ($line =~ /^<abstract>(.*)<\/abstract>/) {
        next if ($1 eq "== 他の紀年法 ==");
        print "$1\n";
        $c++;
        last if $c >= 10000;
    }
}

