dtmc

// number of voters
const int N = 5;

// number of candidates
const int c = 4;


// preference of each voter
global s1 : [0..1];
global secret vot1 : [1..c]; 
global s2 : [0..1];
global secret vot2 : [1..c]; 
global s3 : [0..1];
global secret vot3 : [1..c]; 
global s4 : [0..1];
global secret vot4 : [1..c]; 
global s5 : [0..1];
global secret vot5 : [1..c]; 


// number of votes for each candidate
// public variables
global observable result1 : [0..N];
global observable result2 : [0..N];
global observable result3 : [0..N];
global observable result4 : [0..N];

// module for first voter1
module voter1	
	
    //count the votes
    //voter1 voted to candidate 1
    [] s1=0 & vot1 = 1 & result1<N -> (result1'=result1+1) & (s1'=1);
    //voter1 voted to candidate 2
    [] s1=0 & vot1 = 2 & result2<N -> (result2'=result2+1) & (s1'=1);
    //voter1 voted to candidate 3
    [] s1=0 & vot1 = 3 & result3<N -> (result3'=result3+1) & (s1'=1);
    //voter1 voted to candidate 4
    [] s1=0 & vot1 = 4 & result4<N -> (result4'=result4+1) & (s1'=1);

endmodule

// construct further voters with renaming
module voter2 = voter1 [ vot1=vot2, s1=s2 ] endmodule
module voter3 = voter1 [ vot1=vot3, s1=s3 ] endmodule
module voter4 = voter1 [ vot1=vot4, s1=s4 ] endmodule
module voter5 = voter1 [ vot1=vot5, s1=s5 ] endmodule

// set of initial states
// (voters in their initial state, "vot1, ..., voteN" can be anything)
init  s1=0 & s2=0 & s3=0 & s4=0 & s5=0 & result1=0&result2=0&result3=0&result4=0 endinit

