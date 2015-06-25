/*
 *
 * ****************
 * Copyright 2015 Tiziano Fagni (tiziano.fagni@isti.cnr.it)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ******************
 */

package it.tizianofagni.sparkboost;

import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * @author Tiziano Fagni (tiziano.fagni@isti.cnr.it)
 */
public class AdaBoostMHLearnerExe {
    public static void main(String[] args) {
        Options options = new Options();
        Option opt = Option.builder("b").longOpt("binaryProblem").desc("Indicate if the input dataset contains a binary problem and not a multilabel one").build();
        options.addOption(opt);
        opt = Option.builder("z").longOpt("labels0Based").desc("Indicate if the labels IDs in the dataset to classify are already assigned in the range [0, numLabels-1] included").build();
        options.addOption(opt);
        opt = Option.builder("l").longOpt("enableSparkLogging").desc("Enable logging messages of Spark").build();
        options.addOption(opt);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        String[] remainingArgs = null;
        try {
            cmd = parser.parse(options, args);
            remainingArgs = cmd.getArgs();
            if (remainingArgs.length != 5)
                throw new ParseException("You need to specify all mandatory parameters");
        } catch (ParseException e) {
            System.out.println("Parsing failed.  Reason: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(AdaBoostMHLearnerExe.class.getSimpleName() + " [OPTIONS] <inputFile> <outputFile> <numIterations> <sparkMaster> <parallelismDegree>", options);
            System.exit(-1);
        }

        boolean binaryProblem = false;
        if (cmd.hasOption("b"))
            binaryProblem = true;
        boolean labels0Based = false;
        if (cmd.hasOption("z"))
            labels0Based = true;
        boolean enablingSparkLogging = false;
        if (cmd.hasOption("l"))
            enablingSparkLogging = true;

        String inputFile = remainingArgs[0];
        String outputFile = remainingArgs[1];
        int numIterations = Integer.parseInt(remainingArgs[2]);
        String sparkMaster = remainingArgs[3];
        int parallelismDegree = Integer.parseInt(remainingArgs[4]);

        long startTime = System.currentTimeMillis();

        // Disable Spark logging.
        if (!enablingSparkLogging) {
            Logger.getLogger("org").setLevel(Level.OFF);
            Logger.getLogger("akka").setLevel(Level.OFF);
        }

        // Create and configure Spark context.
        SparkConf conf = new SparkConf().setAppName("Spark MPBoost learner");
        conf.setMaster(sparkMaster);
        conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
        JavaSparkContext sc = new JavaSparkContext(conf);


        // Create and configure learner.
        AdaBoostMHLearner learner = new AdaBoostMHLearner(sc);
        learner.setNumIterations(numIterations);
        learner.setParallelismDegree(parallelismDegree);

        // Build classifier with MPBoost learner.
        BoostClassifier classifier = learner.buildModel(inputFile, labels0Based, binaryProblem);

        // Save classifier to disk.
        DataUtils.saveModel(classifier, outputFile);

        long endTime = System.currentTimeMillis();
        System.out.println("Execution time: " + (endTime - startTime) + " milliseconds.");
    }
}