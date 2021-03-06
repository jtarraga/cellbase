/*
 * Copyright 2015 OpenCB
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
 */

package org.opencb.cellbase.server.ws.feature;

import com.google.common.base.Splitter;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.opencb.biodata.models.core.Gene;
import org.opencb.cellbase.core.db.api.core.GeneDBAdaptor;
import org.opencb.cellbase.core.db.api.regulatory.MirnaDBAdaptor;
import org.opencb.cellbase.core.db.api.regulatory.TfbsDBAdaptor;
import org.opencb.cellbase.core.db.api.systems.ProteinProteinInteractionDBAdaptor;
import org.opencb.cellbase.core.db.api.variation.MutationDBAdaptor;
import org.opencb.cellbase.core.db.api.variation.VariationDBAdaptor;
import org.opencb.cellbase.server.exception.VersionException;
import org.opencb.cellbase.server.ws.GenericRestWSServer;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.datastore.core.QueryResult;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author imedina
 */
@Path("/{version}/{species}/feature/gene")
@Produces("application/json")
@Api(value = "Gene", description = "Gene RESTful Web Services API")
public class GeneWSServer extends GenericRestWSServer {


    public GeneWSServer(@PathParam("version") String version, @PathParam("species") String species,
                        @Context UriInfo uriInfo, @Context HttpServletRequest hsr) throws VersionException, IOException {
        super(version, species, uriInfo, hsr);
    }

    @GET
    @Path("/model")
    public Response getModel() {
        return createModelResponse(Gene.class);
    }

    @GET
    @Path("/first")
    @Override
    public Response first() {
        GeneDBAdaptor geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(this.species, this.assembly);
        return createOkResponse(geneDBAdaptor.first());
    }

    @GET
    @Path("/count")
    @Override
    public Response count() {
        GeneDBAdaptor geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(this.species, this.assembly);
        return createOkResponse(geneDBAdaptor.count());
    }

    @GET
    @Path("/stats")
    @Override
    public Response stats() {
        return super.stats();
    }

    @GET
    @Path("/all")
    @ApiOperation(httpMethod = "GET", value = "Retrieves all the gene objects", response = QueryResponse.class)
    public Response getAll(@ApiParam(value = "String with the list of biotypes to return. Not currently used.")
                           @DefaultValue("") @QueryParam("biotype") List<String> biotypes) {
        try {
            checkParams();
            GeneDBAdaptor geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(this.species, this.assembly);
            if(queryOptions.get("limit") == null || queryOptions.getInt("limit") > 1000) {
                queryOptions.put("limit", 1000);
            }

            return createOkResponse(geneDBAdaptor.getAll(queryOptions));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/list")
    @ApiOperation(httpMethod = "GET", value = "Retrieves all the gene Ensembl IDs")
    public Response getAllIDs(@ApiParam(value = "String with the list of biotypes to return. Not currently used.")
                              @DefaultValue("") @QueryParam("biotype") String biotypes) {
        try {
            checkParams();
            GeneDBAdaptor geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(this.species, this.assembly);
            queryOptions.put("include", Arrays.asList("id"));
            System.out.println(queryOptions);
            if(queryOptions.get("limit") == null || queryOptions.getInt("limit") > 1000) {
                queryOptions.put("limit", 1000);
            }
            return createOkResponse(geneDBAdaptor.getAll(queryOptions));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{geneId}/info")
    @ApiOperation(httpMethod = "GET", value = "Get information about the specified gene(s)")
    public Response getByEnsemblId(@PathParam("geneId") String query) {
        try {
            checkParams();
            GeneDBAdaptor geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(this.species, this.assembly);

            List<org.opencb.datastore.core.QueryResult> genes = geneDBAdaptor.getAllByIdList(Splitter.on(",").splitToList(query), queryOptions);
//            List genes = geneDBAdaptor.getAllByChromosomeIdList(Splitter.on(",").splitToList(query), queryOptions);
//            System.out.println(genes.get(0).getResult().get(0).getClass().toString());
            return createOkResponse(genes);
//			return generateResponse(query, "GENE", geneDBAdaptor.getAllByNameList(StringUtils.toList(query, ","),exclude));
            //	return generateResponse(query, Arrays.asList(this.getGeneDBAdaptor().getAllByEnsemblIdList(StringUtils.toList(query, ","))));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{geneId}/next")
    @ApiOperation(httpMethod = "GET", value = "Get information about the specified gene(s)")
    public Response getNextByEnsemblId(@PathParam("geneId") String query) {
        try {
            checkParams();
            GeneDBAdaptor geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(this.species, this.assembly);

//			QueryOptions queryOptions = new QueryOptions("exclude", exclude);
//			queryOptions.put("include", include );
            QueryResult genes = geneDBAdaptor.next(Splitter.on(",").splitToList(query).get(0), queryOptions);
//            List genes = geneDBAdaptor.getAllByChromosomeIdList(Splitter.on(",").splitToList(query), queryOptions);
//            System.out.println(genes.get(0).getResult().get(0).getClass().toString());
            return createOkResponse(genes);
//			return generateResponse(query, "GENE", geneDBAdaptor.getAllByNameList(StringUtils.toList(query, ","),exclude));
            //	return generateResponse(query, Arrays.asList(this.getGeneDBAdaptor().getAllByEnsemblIdList(StringUtils.toList(query, ","))));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{geneId}/transcript")
    @ApiOperation(httpMethod = "GET", value = "Get the transcripts of a list of gene IDs")
    public Response getTranscriptsByGeneId(@PathParam("geneId") String query) {
        try {
            checkParams();
            GeneDBAdaptor geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(this.species, this.assembly);
            return createOkResponse(geneDBAdaptor.getAllByIdList(Splitter.on(",").splitToList(query), queryOptions));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/biotypes")
    @ApiOperation(httpMethod = "GET", value = "Get the list of existing biotypes")
    public Response getAllBiotypes() {
        try {
            checkParams();
            GeneDBAdaptor geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(this.species, this.assembly);
            return createOkResponse(geneDBAdaptor.getAllBiotypes(queryOptions));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{geneId}/snp")
    @ApiOperation(httpMethod = "GET", value = "Get all SNPs within the specified gene(s)")
    public Response getSNPByGeneId(@PathParam("geneId") String query) {
        try {
            checkParams();

            GeneDBAdaptor geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(this.species, this.assembly);
            VariationDBAdaptor variationDBAdaptor = dbAdaptorFactory.getVariationDBAdaptor(this.species, this.assembly);

            List<org.opencb.datastore.core.QueryResult> qrList = geneDBAdaptor.getAllByIdList(Splitter.on(",").splitToList(query), queryOptions);
            List<QueryResult> queryResults = new ArrayList<>();
            for (org.opencb.datastore.core.QueryResult qr : qrList) {
                QueryResult queryResult = new QueryResult();
                queryResult.setId(qr.getId());

                BasicDBList genes = (BasicDBList) qr.getResult();
                BasicDBObject gene = (BasicDBObject) genes.get(0);
                QueryResult variationQueryResult = variationDBAdaptor.getAllByRegion(gene.getString("chromosome"), gene.getInt("start"), gene.getInt("end"), queryOptions);

                queryResult.setNumResults(variationQueryResult.getNumResults());
                queryResult.setResult(variationQueryResult.getResult());
                queryResults.add(queryResult);
            }

            return createOkResponse(queryResults);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }


    @GET
    @Path("/{geneId}/mutation")
    @ApiOperation(httpMethod = "GET", value = "Get all variants within the specified gene(s)")
    public Response getMutationByGene(@PathParam("geneId") String query) {
        try {
            checkParams();
            MutationDBAdaptor mutationAdaptor = dbAdaptorFactory.getMutationDBAdaptor(this.species, this.assembly);
//            List<List<MutationPhenotypeAnnotation>> geneList = mutationAdaptor.getAllMutationPhenotypeAnnotationByGeneNameList(Splitter.on(",").splitToList(query));
            List<QueryResult> queryResults = mutationAdaptor.getAllByGeneNameList(Splitter.on(",").splitToList(query), queryOptions);
//            return generateResponse(query, "MUTATION", queryResults);
            return createOkResponse(queryResults);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{geneId}/tfbs")
    @ApiOperation(httpMethod = "GET", value = "Get all transcription factor binding sites for this gene(s)")
    public Response getAllTfbs(@PathParam("geneId") String query) {
        try {
            checkParams();
            TfbsDBAdaptor tfbsDBAdaptor = dbAdaptorFactory.getTfbsDBAdaptor(this.species, this.assembly);
            return createOkResponse(tfbsDBAdaptor.getAllByTargetGeneIdList(Splitter.on(",").splitToList(query), queryOptions));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }


    @GET
    @Path("/{geneId}/mirna_target")
    @ApiOperation(httpMethod = "GET", value = "Get all microRNAs binding sites for this gene(s)")
    public Response getAllMirna(@PathParam("geneId") String query) {
        try {
            checkParams();
            MirnaDBAdaptor mirnaDBAdaptor = dbAdaptorFactory.getMirnaDBAdaptor(this.species, this.assembly);
            return generateResponse(query, "MIRNA_TARGET", mirnaDBAdaptor.getAllMiRnaTargetsByGeneNameList(Splitter.on(",").splitToList(query)));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    @GET
    @Path("/{geneId}/exon")
    @ApiOperation(httpMethod = "GET", value = "Get all exons for this gene(s)")
    public Response getExonByGene(@PathParam("geneId") String query) {
        try {
            checkParams();
            GeneDBAdaptor geneDBAdaptor = dbAdaptorFactory.getGeneDBAdaptor(this.species, this.assembly);
            return createOkResponse(geneDBAdaptor.getAllByIdList(Splitter.on(",").splitToList(query), queryOptions));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }


    @GET
    @Path("/{geneId}/protein")
    @ApiOperation(httpMethod = "GET", value = "Get the protein-protein interactions in which this gene is involved")
    public Response getPPIByEnsemblId(@PathParam("geneId") String query) {
        try {
            checkParams();
            ProteinProteinInteractionDBAdaptor PPIDBAdaptor = dbAdaptorFactory.getProteinProteinInteractionDBAdaptor(this.species, this.assembly);
            return createOkResponse(PPIDBAdaptor.getAllByInteractorIdList(Splitter.on(",").splitToList(query), queryOptions));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }



//    @GET
//    @Path("/{geneId}/clinvar")
//    @ApiOperation(httpMethod = "GET", value = "Resource to get ClinVar records from a list of gene HGNC symbols")
//    public Response getAllClinvarByGene(@PathParam("geneId") String query,
//                                       @DefaultValue("") @QueryParam("id") String id,
//                                       @DefaultValue("") @QueryParam("region") String region,
//                                       @DefaultValue("") @QueryParam("phenotype") String phenotype) {
//        try {
//            checkParams();
//            ClinicalDBAdaptor clinicalDBAdaptor = dbAdaptorFactory.getClinicalDBAdaptor(this.species, this.assembly);
//            if(region != null && !region.equals("")) {
//                queryOptions.add("region", Region.parseRegions(query));
//            }
//            if(id != null && !id.equals("")) {
//                queryOptions.add("id", Arrays.asList(id.split(",")));
//            }
//            if(phenotype != null && !phenotype.equals("")) {
//                queryOptions.add("phenotype", Arrays.asList(phenotype.split(",")));
//            }
//
//            return createOkResponse(clinicalDBAdaptor.getAllClinvarByGeneList(Splitter.on(",").splitToList(query), queryOptions));
//        } catch (Exception e) {
//            e.printStackTrace();
//            return createErrorResponse("getAllByAccessions", e.toString());
//        }
//    }

    @GET
    public Response defaultMethod() {
        return help();
    }

    @GET
    @Path("/help")
    public Response help() {
        StringBuilder sb = new StringBuilder();
        sb.append("Input:\n");
        sb.append("all id formats are accepted.\n\n\n");
        sb.append("Resources:\n");
        sb.append("- info: Get gene information: name, position, biotype.\n");
        sb.append(" Output columns: Ensembl gene, external name, external name source, biotype, status, chromosome, start, end, strand, source, description.\n\n");
        sb.append("- transcript: Get all transcripts for this gene.\n");
        sb.append(" Output columns: Ensembl ID, external name, external name source, biotype, status, chromosome, start, end, strand, coding region start, coding region end, cdna coding start, cdna coding end, description.\n\n");
        sb.append("- tfbs: Get transcription factor binding sites (TFBSs) that map to the promoter region of this gene.\n");
        sb.append(" Output columns: TF name, target gene name, chromosome, start, end, cell type, sequence, score.\n\n");
        sb.append("- mirna_target: Get all microRNA target sites for this gene.\n");
        sb.append(" Output columns: miRBase ID, gene target name, chromosome, start, end, strand, pubmed ID, source.\n\n");
        sb.append("- protein_feature: Get protein information related to this gene.\n");
        sb.append(" Output columns: feature type, aa start, aa end, original, variation, identifier, description.\n\n\n");
        sb.append("Documentation:\n");
        sb.append("http://docs.bioinfo.cipf.es/projects/cellbase/wiki/Feature_rest_ws_api#Gene");

        return createOkResponse(sb.toString());
    }

}
