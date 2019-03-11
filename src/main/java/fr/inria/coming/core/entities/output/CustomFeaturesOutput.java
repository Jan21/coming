package fr.inria.coming.core.entities.output;

import com.google.gson.*;
import fr.inria.coming.changeminer.analyzer.commitAnalyzer.FineGrainDifftAnalyzer;
import fr.inria.coming.changeminer.entity.FinalResult;
import fr.inria.coming.codefeatures.CustomFeatureAnalyzer;
import fr.inria.coming.codefeatures.FeatureAnalyzer;
import gumtree.spoon.builder.Json4SpoonGenerator;
import fr.inria.coming.codefeatures.FeaturesResult;
import fr.inria.coming.core.entities.RevisionResult;
import fr.inria.coming.core.entities.interfaces.IOutput;
import fr.inria.coming.main.ComingProperties;
import org.apache.log4j.Logger;
import fr.inria.coming.core.entities.AnalysisResult;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Array;

/**
 * 
 * @author Matias Martinez
 *
 */
public class CustomFeaturesOutput implements IOutput {
	protected static Logger log = Logger.getLogger(Thread.currentThread().getName());
	protected static Json4SpoonGenerator jsonGenerator = new Json4SpoonGenerator();

	@Override
	public void generateFinalOutput(FinalResult finalResult) {

		log.debug("JSON output");
		// JsonObject root = new JsonObject();
		// JsonArray instances = new JsonArray();
		for (Object commit : finalResult.getAllResults().keySet()) {

			RevisionResult rv = (RevisionResult) finalResult.getAllResults().get(commit);

			if (rv == null)
				continue;

			FeaturesResult result = (FeaturesResult) rv.getResultFromClass(CustomFeatureAnalyzer.class);
			if (result == null)
				continue;
			save(result);
		}

	}

	public JsonElement save(FeaturesResult result) {
		JsonElement file = result.getFeatures();

		FileWriter fw;
		try {

			// Create the output dir
			File fout = new File(ComingProperties.getProperty("output"));
			String[] dirsArr = ComingProperties.getProperty("location").split("/");
			String repoName = dirsArr[dirsArr.length-1];
			fout.mkdirs();

			String fileName = fout.getAbsolutePath() + File.separator + "commits_for_repo_" + repoName
					+ ".json";
			fw = new FileWriter(fileName, true);
			Gson gson = new GsonBuilder().create();
			JsonParser jp = new JsonParser();
			JsonElement je = jp.parse(file.toString());
			String prettyJsonString = gson.toJson(je);
			//log.debug("\nJSON Code Change Frequency: (file stored at " + fileName + ")\n");
			//log.debug(prettyJsonString);
			fw.write(prettyJsonString);
			fw.write("\n");

			fw.flush();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}
		return file;
	}

	@Override
	public void generateRevisionOutput(RevisionResult resultAllAnalyzed) {
		FeaturesResult result = (FeaturesResult) resultAllAnalyzed.getResultFromClass(CustomFeatureAnalyzer.class);
		//JsonObject json = jsonGenerator.getJSONwithOperations()
		save(result);
	}

}
