package org.opencb.cellbase.mongodb.db;

import com.mongodb.*;
import org.opencb.biodata.models.feature.Region;
import org.opencb.biodata.models.variant.annotation.Clinvar;
import org.opencb.biodata.models.variant.annotation.Cosmic;
import org.opencb.biodata.models.variant.annotation.Gwas;
import org.opencb.biodata.models.variation.GenomicVariant;
import org.opencb.cellbase.core.common.Position;

import org.opencb.cellbase.core.lib.api.variation.ClinicalDBAdaptor;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.datastore.mongodb.MongoDataStore;

import javax.management.Query;
import java.util.*;

/**
 * Created by antonior on 11/18/14.
 * @author Javier Lopez fjlopez@ebi.ac.uk
 */
public class ClinicalMongoDBAdaptor extends MongoDBAdaptor implements ClinicalDBAdaptor {


    public ClinicalMongoDBAdaptor(DB db) {
        super(db);
    }

    public ClinicalMongoDBAdaptor(DB db, String species, String assembly) {
        super(db, species, assembly);
        mongoDBCollection = db.getCollection("clinical");
        logger.info("ClinicalVarMongoDBAdaptor: in 'constructor'");
    }

    public ClinicalMongoDBAdaptor(String species, String assembly, MongoDataStore mongoDataStore) {
        super(species, assembly, mongoDataStore);
        mongoDBCollection2 = mongoDataStore.getCollection("clinical");

        logger.info("ClinicalMongoDBAdaptor: in 'constructor'");
    }

    @Override
    public QueryResult getAllByPosition(String chromosome, int position, QueryOptions options) {
        //return getAllByRegion(new Region(chromosome, position, position), options);
        return new QueryResult();
    }

    @Override
    public QueryResult getAllByPosition(Position position, QueryOptions options) {
        //return getAllByRegion(new Region(position.getChromosome(), position.getPosition(), position.getPosition()), options);
        return new QueryResult();
    }

    @Override
    public List<QueryResult> getAllByPositionList(List<Position> positionList, QueryOptions options) {
        //List<Region> regions = new ArrayList<>();
        //for (Position position : positionList) {
        //    regions.add(new Region(position.getChromosome(), position.getPosition(), position.getPosition()));
        //}
        //return getAllByRegionList(regions, options);
        return new ArrayList<>();
    }

    @Override
    public QueryResult getClinvarById(String id, QueryOptions options) {
        return getAllClinvarByIdList(Arrays.asList(id), options).get(0);
    }

    @Override
    public List<QueryResult> getAllClinvarByIdList(List<String> idList, QueryOptions options) {
        List<DBObject> queries = new ArrayList<>(idList.size());
        options.addToListOption("include", "clinvarList");
        options.addToListOption("include", "chromosome");
        options.addToListOption("include", "start");
        options.addToListOption("include", "end");
        options.addToListOption("include", "reference");
        options.addToListOption("include", "alternate");
        for (String id : idList) {
            QueryBuilder builder = addClinvarQueryFilters(QueryBuilder.start("clinvarList.clinvarSet.referenceClinVarAssertion.clinVarAccession.acc").is(id),
                    options);
            queries.add(builder.get());
        }

        return prepareClinvarQueryResultList(executeQueryList2(idList, queries, options));
    }

    private QueryBuilder addClinvarQueryFilters(QueryBuilder builder, QueryOptions options) {
        List<Object> genes = options.getList("gene", null);
        BasicDBList geneSymbols = new BasicDBList();
        if (genes != null && genes.size() > 0) {
            geneSymbols.addAll(genes);
            builder = builder.and("clinvarList.clinvarSet.referenceClinVarAssertion.measureSet.measure.measureRelationship.symbol.elementValue.value").
                    in(geneSymbols);
        }
        List<Object> rcvs = options.getList("rcv", null);
        BasicDBList rcvList = new BasicDBList();
        if (rcvs != null && rcvs.size() > 0) {
            rcvList.addAll(rcvs);
            builder = builder.and("clinvarList.clinvarSet.referenceClinVarAssertion.clinVarAccession.acc").
                    in(rcvList);
        }
        List<Object> rs = options.getList("rs", null);
        BasicDBList rsList = new BasicDBList();
        if (rs != null && rs.size() > 0) {
            for(Object rsId : rs) {
                rsList.add(Integer.valueOf(((String) rsId).substring(2)));  // rs id is an integer in clinvar. Remove the starting "rs"
            }
            builder = builder.and("clinvarList.clinvarSet.referenceClinVarAssertion.measureSet.measure.attributeSet.xref.type").
                    is("rs").and("clinvarList.clinvarSet.referenceClinVarAssertion.measureSet.measure.attributeSet.xref.id").in(rsList);
        }
        List<Object> regions = options.getList("region", null);
        BasicDBList regionList = new BasicDBList();
        if (regions != null) {
            Region region = (Region) regions.get(0);
            QueryBuilder regionQueryBuilder = QueryBuilder.start("chromosome").is(region.getChromosome()).and("end")
                    .greaterThanEquals(region.getStart()).and("start").lessThanEquals(region.getEnd());
            for(int i=1; i<regions.size(); i++) {
                region = (Region) regions.get(i);
                regionQueryBuilder = regionQueryBuilder.or(QueryBuilder.start("chromosome").is(region.getChromosome()).and("end")
                        .greaterThanEquals(region.getStart()).and("start").lessThanEquals(region.getEnd()).get());
            }
            builder = builder.and(regionQueryBuilder.get());
        }
        return builder;
    }

    private List<QueryResult> prepareClinvarQueryResultList(List<QueryResult> clinicalQueryResultList) {
        List<QueryResult> queryResultList = new ArrayList<>();
        for(QueryResult clinicalQueryResult: clinicalQueryResultList) {
            QueryResult queryResult = new QueryResult();
            queryResult.setId(clinicalQueryResult.getId());
            queryResult.setDbTime(clinicalQueryResult.getDbTime());
            BasicDBList basicDBList = new BasicDBList();
            int numResults = 0;
            for (BasicDBObject clinicalRecord : (List<BasicDBObject>) clinicalQueryResult.getResult()) {
                if(clinicalRecord.containsKey("clinvarList")) {
                    basicDBList.add(clinicalRecord);
                    numResults += 1;
                }
            }
            queryResult.setResult(basicDBList);
            queryResult.setNumResults(numResults);
            queryResultList.add(queryResult);
        }
        return queryResultList;
    }

    @Override
    public QueryResult getAllClinvarByRegion(String chromosome, int start, int end, QueryOptions options) {
        return getAllClinvarByRegion(new Region(chromosome, start, end), options);
    }

    @Override
    public QueryResult getAllClinvarByRegion(Region region, QueryOptions options) {
        return getAllClinvarByRegionList(Arrays.asList(region), options).get(0);
    }

    @Override
    public List<QueryResult> getAllClinvarByRegionList(List<Region> regions, QueryOptions options) {
        List<DBObject> queries = new ArrayList<>();

        options.addToListOption("include", "clinvarList");
        options.addToListOption("include", "chromosome");
        options.addToListOption("include", "start");
        options.addToListOption("include", "end");
        options.addToListOption("include", "reference");
        options.addToListOption("include", "alternate");
        List<String> ids = new ArrayList<>(regions.size());
        for (Region region : regions) {

            // If regions is 1 position then query can be optimize using chunks
            QueryBuilder builder = QueryBuilder.start("chromosome").is(region.getChromosome()).and("end").greaterThanEquals(region.getStart()).and("start").lessThanEquals(region.getEnd());
            builder = addClinvarQueryFilters(builder, options);
            System.out.println(builder.get().toString());
            queries.add(builder.get());
            ids.add(region.toString());
        }
        return prepareClinvarQueryResultList(executeQueryList2(ids, queries, options));
    }

    @Override
    public QueryResult getAllByGenomicVariant(GenomicVariant variant, QueryOptions options) {
        return getAllByGenomicVariantList(Arrays.asList(variant), options).get(0);
    }

    @Override
    public List<QueryResult> getAllByGenomicVariantList(List<GenomicVariant> variantList, QueryOptions options) {
        List<DBObject> queries = new ArrayList<>();
        List<String> ids = new ArrayList<>(variantList.size());
        List<QueryResult> queryResultList;
        for (GenomicVariant genomicVariant : variantList){
            QueryBuilder builder = QueryBuilder.start("chromosome").is(genomicVariant.getChromosome()).
                    and("start").is(genomicVariant.getPosition()).and("alternate").is(genomicVariant.getAlternative());
            if (genomicVariant.getReference() != null){
                builder = builder.and("reference").is(genomicVariant.getReference());
            }
                    queries.add(builder.get());
            ids.add(genomicVariant.toString());
        }

        queryResultList = executeQueryList2(ids, queries, options);


        for (QueryResult queryResult : queryResultList){
            List<BasicDBObject> clinicalList = (List<BasicDBObject>) queryResult.getResult();
            Cosmic cosmic;
            Gwas gwas;
            Clinvar clinvar;
            Map<String, Object> clinicalData = new HashMap<>();
            List<Cosmic> cosmicLisit = new ArrayList<>();
            List<Gwas> gwasList = new ArrayList<>();
            List<Clinvar> clinvarList = new ArrayList<>();

            for(Object clinicalObject: clinicalList) {
                BasicDBObject clinical = (BasicDBObject) clinicalObject;
                List<BasicDBObject> consmicObjList = (List<BasicDBObject>) clinical.get("cosmicList");
                List<BasicDBObject> gwasObjList = (List<BasicDBObject>) clinical.get("gwasList");
                List<BasicDBObject> clinvarObjList = (List<BasicDBObject>) clinical.get("clinvarList");

                if (consmicObjList != null){
                    for (BasicDBObject cosmicObj : consmicObjList){

                        String mutationID = (String) cosmicObj.get("mutationID");
                        String primarySite = (String) cosmicObj.get("primarySite");
                        String siteSubtype = (String) cosmicObj.get("siteSubtype");
                        String primaryHistology = (String) cosmicObj.get("primaryHistology");
                        String histologySubtype = (String) cosmicObj.get("histologySubtype");
                        String sampleSource = (String) cosmicObj.get("sampleSource");
                        String tumourOrigin = (String) cosmicObj.get("tumourOrigin");
                        String geneName = (String) cosmicObj.get("geneName");
                        String mutationSomaticStatus = (String) cosmicObj.get("mutationSomaticStatus");

                        cosmic = new Cosmic(mutationID, primarySite, siteSubtype, primaryHistology,
                                histologySubtype, sampleSource, tumourOrigin ,geneName, mutationSomaticStatus);
                        cosmicLisit.add(cosmic);
                        clinicalData.put("Cosmic",cosmicLisit);
                    }
                }else {
                    clinicalData.put("Cosmic",null);
                }
                if (gwasObjList != null) {
                    for (BasicDBObject gwasObj : gwasObjList) {
                        String snpIdCurrent = (String) gwasObj.get("snpIdCurrent");
                        Double riskAlleleFrequency =  (Double) gwasObj.get("riskAlleleFrequency");
                        String reportedGenes = (String) gwasObj.get("reportedGenes");
                        List<BasicDBObject> studiesObj = (List<BasicDBObject>) gwasObj.get("studies");
                        Set<String> traitsSet = new HashSet<>();

                        for (BasicDBObject studieObj: studiesObj) {
                            List<BasicDBObject> traitsObj = (List<BasicDBObject>) studieObj.get("traits");
                            for (BasicDBObject traitObj : traitsObj) {
                                String trait =(String) traitObj.get("diseaseTrait");
                                traitsSet.add(trait);
                            }
                        }

                        List<String>  traits = new ArrayList<>();
                        traits.addAll(traitsSet);
                        gwas = new Gwas(snpIdCurrent,traits,riskAlleleFrequency,reportedGenes);

                        gwasList.add(gwas);
                        clinicalData.put("Gwas",gwasList);
                    }
                }else {
                    clinicalData.put("Gwas",null);
                }

                if (clinvarObjList != null ){
                    for (BasicDBObject clinvarObj : clinvarObjList){

                        BasicDBObject clinvarSet = (BasicDBObject) clinvarObj.get("clinvarSet");
                        BasicDBObject referenceClinVarAssertion = (BasicDBObject) clinvarSet.get("referenceClinVarAssertion");
                        BasicDBObject clinVarAccession = (BasicDBObject) referenceClinVarAssertion.get("clinVarAccession");
                        BasicDBObject clinicalSignificance = (BasicDBObject) referenceClinVarAssertion.get("clinicalSignificance");
                        BasicDBObject measureSet = (BasicDBObject) referenceClinVarAssertion.get("measureSet");
                        List<BasicDBObject> measures = (List<BasicDBObject>) measureSet.get("measure");
                        BasicDBObject traitSet = (BasicDBObject) referenceClinVarAssertion.get("traitSet");
                        List<BasicDBObject> traits = (List<BasicDBObject>) traitSet.get("trait");


                        String acc = (String)  clinVarAccession.get("acc");
                        String clinicalSignificanceName = (String) clinicalSignificance.get("description");
                        String reviewStatus = (String) clinicalSignificance.get("reviewStatus");
                        List <String> traitNames = new ArrayList<>();
                        Set <String> geneNameSet = new HashSet<>();

                        for (BasicDBObject measure : measures){
                            List <BasicDBObject> measureRelationships;
                            if((measureRelationships = (List<BasicDBObject>) measure.get("measureRelationship"))!=null) {
                                for (BasicDBObject measureRelationship : measureRelationships) {
                                    List<BasicDBObject> symbols = (List<BasicDBObject>) measureRelationship.get("symbol");
                                    for (BasicDBObject symbol : symbols) {
                                        BasicDBObject elementValue = (BasicDBObject) symbol.get("elementValue");
                                        geneNameSet.add((String) elementValue.get("value"));
                                    }
                                }
                            }
                        }

                        for (BasicDBObject trait : traits){
                            List <BasicDBObject> names = (List<BasicDBObject>) trait.get("name");
                            for (BasicDBObject name: names){
                                    BasicDBObject elementValue = (BasicDBObject) name.get("elementValue");
                                    traitNames.add((String) elementValue.get("value"));
                                }
                            }

                        List<String>  geneNameList = new ArrayList<>();
                        geneNameList.addAll(geneNameSet);
                        clinvar = new Clinvar(acc,clinicalSignificanceName, traitNames, geneNameList, reviewStatus);
                        clinvarList.add(clinvar);
                    }

                    clinicalData.put("Clinvar", clinvarList);
                }else {
                    clinicalData.put("Clinvar", null);
                }
            }

            // FIXME quick solution to compile
//            queryResult.setResult(clinicalData);
            queryResult.setResult(Arrays.asList(clinicalData));
        }

        return queryResultList;
    }

    public QueryResult getListClinvarAccessions(QueryOptions queryOptions) {
        QueryBuilder builder = QueryBuilder.start("clinvarList.clinvarSet.referenceClinVarAssertion.clinVarAccession.acc").exists(true);
        queryOptions.put("include", Arrays.asList("clinvarList.clinvarSet.referenceClinVarAssertion.clinVarAccession.acc"));
        QueryResult queryResult = executeQuery("", builder.get(), queryOptions);
        List accInfoList = (List) queryResult.getResult();
        List<String> accList = new ArrayList<>(accInfoList.size());
        BasicDBObject accInfo;
        QueryResult listAccessionsToReturn = new QueryResult();

        for(Object accInfoObject: accInfoList) {
            accInfo = (BasicDBObject) accInfoObject;
            if(accInfo.containsKey("clinvarList")) {
                accInfo = (BasicDBObject)((BasicDBObject) ((List) accInfo.get("clinvarList"))
                        .get(0)).get("clinvarSet");
                accList.add((String) ((BasicDBObject) ((BasicDBObject) ((BasicDBObject) accInfo
                        .get("referenceClinVarAssertion"))).get("clinVarAccession")).get("acc"));
            }
        }

        // setting listAccessionsToReturn fields
        listAccessionsToReturn.setId(queryResult.getId());
        listAccessionsToReturn.setDbTime(queryResult.getDbTime());
        listAccessionsToReturn.setNumResults(queryResult.getNumResults());
        listAccessionsToReturn.setNumTotalResults(queryResult.getNumTotalResults());
        listAccessionsToReturn.setResult(accList);

        return listAccessionsToReturn;
    }

}
