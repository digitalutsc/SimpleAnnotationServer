package uk.org.llgc.annotation.store.adapters;

import org.apache.jena.query.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.Lang;

import uk.org.llgc.annotation.store.data.PageAnnoCount;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;

import com.github.jsonldjava.utils.JsonUtils;

import java.nio.charset.Charset;

public class JenaStore extends AbstractRDFStore implements StoreAdapter {
	protected static Logger _logger = LogManager.getLogger(JenaStore.class.getName());

	protected Dataset _dataset = null;

	public JenaStore(final String pDataDir) {
		_dataset = TDBFactory.createDataset(pDataDir);
	}

	public Model addAnnotationSafe(final Map<String,Object> pJson) throws IOException {
		String tJson = JsonUtils.toString(pJson);

        _logger.debug("Converting: " + tJson);
		Model tJsonLDModel = ModelFactory.createDefaultModel();

		RDFDataMgr.read(tJsonLDModel, new ByteArrayInputStream(tJson.getBytes(Charset.forName("UTF-8"))), Lang.JSONLD);


		_dataset.begin(ReadWrite.WRITE) ;
		_dataset.addNamedModel((String)pJson.get("@id"), tJsonLDModel);
		_dataset.commit();

		return tJsonLDModel;
	}

	public void deleteAnnotation(final String pAnnoId) throws IOException {
		_dataset.begin(ReadWrite.WRITE) ; // should probably move this to deleted state
		_dataset.removeNamedModel(pAnnoId);
		_dataset.commit();
	}

	protected QueryExecution getQueryExe(final String pQuery) {
		return QueryExecutionFactory.create(pQuery, _dataset);
	}
	protected Model getNamedModel(final String pContext) throws IOException {
		boolean tLocaltransaction = !_dataset.isInTransaction();
		if (tLocaltransaction) {
			_dataset.begin(ReadWrite.READ);
		}
		Model tAnnotation = _dataset.getNamedModel(pContext);
		if (tAnnotation.isEmpty()) {
			tAnnotation = null; // annotation wasn't found
		}
		if (tLocaltransaction) {
			_dataset.end();
		}
        return tAnnotation;
	}

	protected void begin(final ReadWrite pWrite) {
		_dataset.begin(pWrite);
	}
	protected void end() {
		_dataset.end();
	}

	protected String indexManifestOnly(final String pShortId, Map<String,Object> pManifest) throws IOException {
		Model tJsonLDModel = ModelFactory.createDefaultModel();
		RDFDataMgr.read(tJsonLDModel, new ByteArrayInputStream(JsonUtils.toString(pManifest).getBytes(Charset.forName("UTF-8"))), Lang.JSONLD);

		//RDFDataMgr.write(System.out, tJsonLDModel, Lang.NQUADS);
		_dataset.begin(ReadWrite.WRITE) ;
		_dataset.addNamedModel((String)pManifest.get("@id"), tJsonLDModel);

		_dataset.commit();

		return pShortId;
	}
}
