package edu.uwm.jiaoduan.algorithms;
/**
 * ref: http://en.wikipedia.org/wiki/Viterbi_algorithm
 */
import java.util.Hashtable;

public class Viterbi 
{
        static final String RAINY = "Rainy";
        static final String SUNNY = "Sunny";
 
        static final String WALK = "walk";
        static final String SHOP = "shop";
        static final String CLEAN = "clean";
 
        public static void main(String[] args) 
        {
                String[] states = new String[] {RAINY, SUNNY};
 
                String[] observations = new String[] {WALK, SHOP, CLEAN};
 
                Hashtable<String, Float> start_probability = new Hashtable<String, Float>();
                start_probability.put(RAINY, 0.6f);
                start_probability.put(SUNNY, 0.4f);
 
                // transition_probability
                Hashtable<String, Hashtable<String, Float>> transition_probability = 
                        new Hashtable<String, Hashtable<String, Float>>();
                        Hashtable<String, Float> t1 = new Hashtable<String, Float>();
                        t1.put(RAINY, 0.7f);
                        t1.put(SUNNY, 0.3f);
                        Hashtable<String, Float> t2 = new Hashtable<String, Float>();
                        t2.put(RAINY, 0.4f);
                        t2.put(SUNNY, 0.6f);
                transition_probability.put(RAINY, t1);
                transition_probability.put(SUNNY, t2);
 
                // emission_probability
                Hashtable<String, Hashtable<String, Float>> emission_probability = 
                        new Hashtable<String, Hashtable<String, Float>>();
                        Hashtable<String, Float> e1 = new Hashtable<String, Float>();
                        e1.put(WALK, 0.1f);         
                        e1.put(SHOP, 0.4f); 
                        e1.put(CLEAN, 0.5f);
                        Hashtable<String, Float> e2 = new Hashtable<String, Float>();
                        e2.put(WALK, 0.6f);         
                        e2.put(SHOP, 0.3f); 
                        e2.put(CLEAN, 0.1f);
                emission_probability.put(RAINY, e1);
                emission_probability.put(SUNNY, e2);
 
                Object[] ret = forward_viterbi(observations,
                           states,
                           start_probability,
                           transition_probability,
                           emission_probability);
                System.out.println(((Float) ret[0]).floatValue());              
                System.out.println((String) ret[1]);
                System.out.println(((Float) ret[2]).floatValue());
        }
 
        public static Object[] forward_viterbi(String[] obs, String[] states,
                        Hashtable<String, Float> start_p,
                        Hashtable<String, Hashtable<String, Float>> trans_p,
                        Hashtable<String, Hashtable<String, Float>> emit_p)
        {
                Hashtable<String, Object[]> T = new Hashtable<String, Object[]>();
                for (String state : states)
                        T.put(state, new Object[] {start_p.get(state), state, start_p.get(state)});
 
                for (String output : obs)
                {
                        Hashtable<String, Object[]> U = new Hashtable<String, Object[]>();
                        for (String next_state : states)
                        {
                                float total = 0;
                                String argmax = "";
                                float valmax = 0;
 
                                float prob = 1;
                                String v_path = "";
                                float v_prob = 1;   
 
                                for (String source_state : states)
                                {
                                        Object[] objs = T.get(source_state);
                                        prob = ((Float) objs[0]).floatValue();
                                        v_path = (String) objs[1];
                                        v_prob = ((Float) objs[2]).floatValue();
 
                                        float p = emit_p.get(source_state).get(output) *
                                                          trans_p.get(source_state).get(next_state);
                                        prob *= p;
                                        v_prob *= p;
                                        total += prob;
                                        if (v_prob > valmax)
                                        {
                                                argmax = v_path + "," + next_state;
                                                valmax = v_prob;
                                        }
                                }
                                U.put(next_state, new Object[] {total, argmax, valmax});
                        }
                        T = U;                        
                }
 
                float total = 0;
                String argmax = "";
                float valmax = 0;
 
                float prob;
                String v_path;
                float v_prob;
 
                for (String state : states)
                {
                        Object[] objs = T.get(state);
                        prob = ((Float) objs[0]).floatValue();
                        v_path = (String) objs[1];
                        v_prob = ((Float) objs[2]).floatValue();
                        total += prob;
                        if (v_prob > valmax)
                        {
                                argmax = v_path;
                                valmax = v_prob;
                        }
                } 
                return new Object[]{total, argmax, valmax};   
        }
}