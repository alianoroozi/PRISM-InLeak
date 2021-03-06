package prismfinalleak;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import jdd.JDDNode;
import jdd.JDDVars;
import odd.ODDNode;
import prism.PrismException;
import prism.ProbModel;
import prism.StateListMTBDD;
import prismintertrace.ExplicitState;
import sparse.PrismSparse;

/**
*
*	A class for explicit representation and output exploration of ProbModel
*
* @author Ali A. Noroozi
*/

public class ProbModelOutputExplorer {
	
	private ProbModel currentModel = null;
	List<ExplicitState> reachStates; // set of reachable states
	List<ExplicitState> startStates; // set of initial states
	
	private Map<String, Map<String, Double>> outSecretDist; // the distribution Pr(o, h), containing output-secret probabilities: Pr(o=\bar{o}, h=\bar{h}) 
	
	public static int UNIFORM_PRIOR_KNOWLEDGE = 0; // probability distribution of the secret variable not specified by the user -> uniform distribution assumed
	public static int INIT_DIST_FILE_PRIOR_KNOWLEDGE = 1; // probability distribution of the secret variable is imported from a file specified by the user
	private int priorKnowledgeType = UNIFORM_PRIOR_KNOWLEDGE; 
	Map<String, Double> priorKnowledge; // probability distribution of the secret variable
	
	JDDNode matrix;
	String name;
	JDDVars rows;
	JDDVars cols;
	ODDNode odd;
		
	public ProbModelOutputExplorer(ProbModel currentModel, String initDistFileName) throws PrismException {
		
		this.currentModel = currentModel;
		this.reachStates = getStates();
		this.startStates = getInitialStates();
//		this.savePathProbs = savePathProbs;
		
		if (initDistFileName == null) { // probability distribution of the secret variable not specified by the user
			priorKnowledgeType = UNIFORM_PRIOR_KNOWLEDGE;
    		this.priorKnowledge = uniformPriorKnowledge();
		}
		else {
			priorKnowledgeType = INIT_DIST_FILE_PRIOR_KNOWLEDGE; // import probability distribution from initDistFileName
			this.priorKnowledge = readInitDistribution(initDistFileName);
		}
			
		outSecretDist = new HashMap<>();
		
		matrix = currentModel.getTrans();
		name = currentModel.getTransSymbol();
		rows = currentModel.getAllDDRowVars();
		cols = currentModel.getAllDDColVars();
		odd = currentModel.getODD();
		
	}
	
	/**
	 * DFS exploration of the model to determine outputs and output-secret probabilities
	 * 
	 */
	public void exploreModel(boolean bounded, int boundedStep) throws PrismException {
		
		double res = PrismSparse.PS_CreateSparseMatrix(matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
		if (res == -2) {
			throw new PrismException("Out of memory building transition matrix");
		}
		
         
    	if(bounded) { // bounded
    		
    		long path[] = new long[boundedStep+2];
    		for (ExplicitState s : startStates)
    			explorePathsRecur(s.getStateNumber(), path, 0, bounded, boundedStep);
    	}
    	else // explore whole paths (till final states) 
    		for (ExplicitState s : startStates)
    			explorePathsNonRecur(s.getStateNumber());
        
//        PrismSparse.PS_FreeSparseMatrix(matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
    	
    	return;
    }
	
	/**
     * Recursive helper function for explorePaths().
     * Given a state, and an array containing the path from the initial state 
     * down to but not including this state, explore all the paths.
     * 
     */
    private void explorePathsRecur(long s, long path[], int path_length, boolean bounded, int boundedStep) {
  
        /* append this state to the path array */
        path[path_length] = s;
        path_length++; // path_length=number of states of the path
  
        /* it's a final state, so add the path that led to here to path_list */
        if ((!bounded && isFinalState(s)) || 
        		(bounded && path_length == boundedStep + 1)){
        	
            List<Long> pa = new ArrayList<>();
            for(int i=0; i<path_length; i++)
                pa.add(path[i]);
            
            handlePath(pa);
        }
        else
            /* try subtrees */
            for(long ps: post(s)) 
            	explorePathsRecur(ps, path, path_length, bounded, boundedStep);
    }    
  
	 /**
	  * An iterative function to do pre-order traversal of the machine and add 
	  * output to outputs list without using recursion
	  * 
	  */
	 void explorePathsNonRecur(long initial)
	 {
		 long current;
		 
	     // Create an empty stack and push initial to it
	     Stack<Long> nodeStack = new Stack<>();
	     nodeStack.push(initial);
	  
	     // Create a map to store parent states of each state
	     Map<Long, Long> parent = new HashMap<>();
	  
	     // parent of initial is NULL
	     parent.put(initial, (long) -1);
	  
	     // Pop all items one by one and push their successors
	     while (!nodeStack.empty())
	     {
	         // Pop the top item from stack
	         current = nodeStack.pop();
	  
	         // If final state encountered, add output to out_list
	         if (isFinalState(current)) {
	             addTopToBottomPath(current, parent);
	         }
	  
	         // Push successors of the popped state to stack. Also set their parent state in the map
	         else
	             for(long ps: post(current)) {
	            	 if(ps != current) {
	            		 parent.put(ps, current);
	            		 nodeStack.push(ps);
	            	 }
	             }
	     }
	 }
    
	 /**
	  * Function to add initial-to-final path for a final state using parent states stored in the parent Map
	  *
	  */
	 void addTopToBottomPath(long curr, Map<Long, Long> parent)
	 {
		 Stack<Long> stk = new Stack<>();
	  
	     // start from final state and keep on pushing nodes into stack till initial state is reached
	     while (curr != -1)
	     {
	         stk.push(curr);
	         curr = parent.get(curr);
	     }
	     
	     List<Long> pa = new ArrayList<>();
	     // Start popping sates from stack and build a path in order to add it to path_list
	     while (!stk.empty())
	     {
	         curr = stk.pop();
	         pa.add(curr);
	     }
	     
	     handlePath(pa);
	     
	     return;
    	 	     
	 }
	 
	 /**
	  * Add probability of the path pa to outProbs
	  * 
	  */
	 public void handlePath(List<Long> pa) {
		 
		 double prob_pa = prob(pa);
		 long finalState = pa.get(pa.size()-1);
		 String output = reachStates.get((int) finalState).getPublicData(-1);
		 
		 Map<String, Double> probs = outSecretDist.getOrDefault(output, new HashMap<>());
		 long startSt = pa.get(0);
		 String secretStartSt = reachStates.get((int)startSt).getSecretData();
	     probs.put(secretStartSt, probs.getOrDefault(secretStartSt, 0.0) + prob_pa);
	     outSecretDist.put(output, probs);
    	 
    	 return;
	 }
	 
	 /**
	  * 
	  * @return the explicit set of reachable states 
	  */
	 public List<ExplicitState> getStates() {
		 
		 StateListMTBDD states = (StateListMTBDD) currentModel.getReachableStates();
		 return states.getExplicitStates();
	 }

	 /**
	  * 
	  * @return the explicit set of initial states 
	  */
	 public List<ExplicitState> getInitialStates() {
		 
		 StateListMTBDD start = (StateListMTBDD) currentModel.getStartStates();
		 return start.getExplicitStates();
	 }
	 
	 /**
	  * 
	  * @return successor states of s. If s has a self-loop, it is included in post(s)
	  */
	 public int[] post(long s) {
		 
		 return PrismSparse.PS_SuccessorStates((int) s, matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
	 }
	 
	 /**
	  * 
	  * @return true if s has no successor or the only successor is itself
	  */
	 public boolean isFinalState(long s) {
		 
		 return PrismSparse.isFinalState((int) s, matrix, name, rows, cols, odd);
	 }
	 
	 /**
	  * 
	  * @return transition probability between states i and j
	  */
	 public double getTransitionProb(long i, long j) {
		 
		 return PrismSparse.PS_GetTransitionProb((int) i, (int) j, matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
	 }
	 
	 /**
	  * 
	  * @return probability of path. Probability of the initial state is also included.
	  */
	 public double prob(List<Long> path) {
		
        double prob = 1.0;
        for(int i=0; i < path.size()-1; i++)
            prob = prob *  getTransitionProb(path.get(i), path.get(i+1));
        
        double muInit;
	    if (priorKnowledgeType == UNIFORM_PRIOR_KNOWLEDGE) // uniform prior knowledge
	    	 muInit = 1.0 / startStates.size();
	    else { // prior knowledge determined by the user (It may be uniform or not)
		    long startSt = path.get(0);
		    muInit = priorKnowledge.get(reachStates.get((int)startSt).getSecretData());
	    }
        return muInit*prob;
	 }
	 
	 /**
	  * Read probability distribution of the secret variable (prior knowledge) from initDistFileName and return it
	  */
	 public Map<String, Double> readInitDistribution(String initDistFileName) throws PrismException{
		 
		 Map<String, Double> priorKnowledge = new HashMap<>();
		 List<Double> initDist = new ArrayList<>();
		 double initDistSum = 0.0; 
					 
		 BufferedReader in;
		 String l;
		 int lineNum = 0;
		 double d;
		
		 try {
			// open file for reading
			 in = new BufferedReader(new FileReader(initDistFileName));
			 // read remaining lines
			 l = in.readLine(); lineNum++;
			 while (l != null) {
				 l = l.trim();
				 if (!("".equals(l))) {
					 d = Double.parseDouble(l);
					 initDist.add(d);
					 initDistSum += d;
				 }
				 l = in.readLine(); lineNum++;
			 }
			 // Close file
			 in.close();

			 int startSize = startStates.size();
			 if(initDist.size() != startSize)
				 throw new PrismException("initDist file should contain " + startSize + " probabilities");
			 
			 if(Math.abs(initDistSum - 1.0) > 0.001)
				 throw new PrismException("Sume of probabilities in \"" + initDistFileName + "\" should be equal to 1.0");
			 
			 int i=0;
			 for(ExplicitState s: startStates) {
				 priorKnowledge.put(s.getSecretData(), initDist.get(i));
				 i++;
			 }
			 
		 } catch (IOException e) {
			 throw new PrismException("File I/O error reading from \"" + initDistFileName + "\"");
		 }
		 catch (NumberFormatException e) {
			 throw new PrismException("Error detected at line " + lineNum + " of file \"" + initDistFileName + "\"");
		 }
		
		 return priorKnowledge;
	 }
	 
	 /**
	  * 
	  * @return uniform probability distribution for the secret values of initial states
	  */
	 public Map<String, Double> uniformPriorKnowledge() {
	    
		 Map<String, Double> uniformPriorKnowledge = new TreeMap<>();
		 
		 for(ExplicitState s: startStates) {
			 uniformPriorKnowledge.put(s.getSecretData(), 1.0/startStates.size());
		 }
		
		return uniformPriorKnowledge;
	 }
	 
	 /**
	  * 
	  * @return the distribution Pr(o,h), containing output-secret probabilities
	  */
	 public Map<String, Map<String, Double>> getOutSecretDist() {
		 
		 return outSecretDist;
	 }
	 	 
	 /**
	  * 
	  * @return prior knowledge Pr(h), which is the probability distribution of the secret values
	  */
	 public Map<String, Double> getPriorKnowledge() {
		 
		 return priorKnowledge;
	 }
	
}

