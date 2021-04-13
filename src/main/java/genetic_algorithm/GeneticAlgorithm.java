package genetic_algorithm;

import java.util.*;

public class GeneticAlgorithm<M extends GeneticAlgorithmMember<M>> {
    public static  final double CROSSOVER_PROBABILITY = 0.2;
    public final static int POPULATION_SIZE = 100;

    private final List<M> population;

    private int generationNumber = 0;

    public GeneticAlgorithm(List<M> population) {
        if (population.size() != POPULATION_SIZE) {
            System.out.println("Ups, population size doesn't match");
        }
        this.population = population;
        evaluate();
    }


    public void nextGeneration() {
        generationNumber++;
        mutate();
        crossover();
        evaluate();
        select();

        System.out.println("Best score: " + population.get(0).fitness() + "\n");
    }

    private void mutate() {
        for (M member : population) {
            member.mutate();
        }
    }

    private void crossover() {
        List<M> crossoverPopulation = new ArrayList<>();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            double r = generateRandom(0.0, 1.0);

            if (r < CROSSOVER_PROBABILITY) {
                M member = population.get(i);
                crossoverPopulation.add(member);
            }
        }

        for (int i = 0; i < crossoverPopulation.size() - 1; i+=2) {
            M memberA = crossoverPopulation.get(i);
            M memberB = crossoverPopulation.get(i + 1);

            List<M> offspring = memberA.makeOffspring(memberB);

            population.add(offspring.get(0));
            population.add(offspring.get(1));
        }

    }

    /* TODO selection wheel */
    private void select() {
        Collections.sort(population);

        int membersToRemove = population.size() - POPULATION_SIZE;

        for (int i = 0; i < membersToRemove; i++) {
            population.remove(population.size() - 1);
        }
    }

    private void evaluate() {
        for (M member : population) {
            member.evaluate();
        }
    }

    private double generateRandom(double start, double end) {
        Random random = new Random();
        return start + (end - start) * random.nextDouble();
    }

    public int getGeneration() {
        return generationNumber;
    }
}
