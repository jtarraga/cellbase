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

package org.opencb.cellbase.server.ws;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.google.common.base.Splitter;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.opencb.biodata.models.core.Gene;
import org.opencb.cellbase.core.CellBaseConfiguration;
import org.opencb.cellbase.core.db.DBAdaptorFactory;
import org.opencb.cellbase.core.db.api.core.GenomeDBAdaptor;
import org.opencb.cellbase.mongodb.db.MongoDBAdaptorFactory;
import org.opencb.cellbase.server.exception.SpeciesException;
import org.opencb.cellbase.server.exception.VersionException;
import org.opencb.datastore.core.ObjectMap;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResponse;
import org.opencb.datastore.core.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@Path("/{version}/{species}")
@Produces("text/plain")
@Api(value = "Generic", description = "Generic RESTful Web Services API")
public class GenericRestWSServer implements IWSServer {

    // Common application parameters
    @DefaultValue("")
    @PathParam("version")
    @ApiParam(name = "version", value = "CellBase version. Use 'latest' for last version stable.",
            allowableValues = "v3,latest", defaultValue = "v3")
    protected String version;

    @DefaultValue("")
    @PathParam("species")
    @ApiParam(name = "species", value = "Name of the species to query",
            defaultValue = "hsapiens", allowableValues = "hsapiens,mmusculus,drerio,rnorvegicus,ptroglodytes,ggorilla,pabelii," +
            "mmulatta,sscrofa,cfamiliaris,ecaballus,ocuniculus,ggallus,btaurus,fcatus,cintestinalis,ttruncatus,lafricana,cjacchus," +
            "nleucogenys,aplatyrhynchos,falbicollis,celegans,dmelanogaster,dsimulans,dyakuba,agambiae,adarlingi,nvectensis," +
            "spurpuratus,bmori,aaegypti,apisum,scerevisiae,spombe,afumigatus,aniger,anidulans,aoryzae,foxysporum,pgraminis," +
            "ptriticina,moryzae,umaydis,ssclerotiorum,cneoformans,ztritici,pfalciparum,lmajor,ddiscoideum,pinfestans,glamblia," +
            "pultimum,alaibachii,athaliana,alyrata,bdistachyon,osativa,gmax,vvinifera,zmays,hvulgare,macuminata,sbicolor,sitalica," +
            "taestivum,brapa,ptrichocarpa,slycopersicum,stuberosum,smoellendorffii,creinhardtii,cmerolae,ecolimg1655,spneumoniae70585," +
            "sagalactiaenem316,saureusst398,saureusn315,smelilotiak83,sfrediingr234,sentericact18,sentericalt2,pluminescenstto1," +
            "nmeningitidisz2491,mgenitaliumg37,mtuberculosisasm19595v2,mavium104,lmonocytogenesegde,lplantarumwcfs1,hinfluenzaekw20," +
            "cglutamicumasm19633v1,cbotulinumhall,ctrachomatisduw3cx,blongumncc2705,bsubtilis168,blicheniformisasm1164v1," +
            "abaumanniiaye,paeruginosapa7,paeruginosapa14,paeruginosampao1p1,paeruginosampao1p2,cpneumoniaecwl029,pacanthamoebaeuv7," +
            "wchondrophila861044,cprotochlamydiauwe25,snegevensisz,csabeus,oaries,olatipes")
    protected String species;

    protected String assembly = null;
    protected UriInfo uriInfo;
    protected HttpServletRequest httpServletRequest;

    protected QueryOptions queryOptions;

    // file name without extension which server will give back when file format is !null
    private String filename;

    @DefaultValue("")
    @QueryParam("exclude")
    @ApiParam(name = "excluded fields", value = "Fields excluded in response. Whole JSON path e.g.: transcripts.id")
    protected String exclude;

    @DefaultValue("")
    @QueryParam("include")
    @ApiParam(name = "included fields", value = "Only fields included in response. Whole JSON path e.g.: transcripts.id")
    protected String include;

    @DefaultValue("-1")
    @QueryParam("limit")
    @ApiParam(name = "limit", value = "Max number of results to be returned. No limit applied when -1. No limit is set by default.")
    protected int limit;

    @DefaultValue("-1")
    @QueryParam("skip")
    @ApiParam(name = "skip", value = "Number of results to be skipped. No skip applied when -1. No skip by default.")
    protected int skip;

    @DefaultValue("false")
    @QueryParam("count")
    @ApiParam(name = "count", value = "Get a count of the number of results obtained. Deactivated by default.",
            defaultValue = "false", allowableValues = "false,true")
    protected String count;

    @DefaultValue("json")
    @QueryParam("of")
    @ApiParam(name = "Output format", value = "Output format, Protobuf is not yet implemented", defaultValue = "json", allowableValues = "json,pb (Not implemented yet)")
    protected String outputFormat;

    protected static ObjectMapper jsonObjectMapper;
    protected static ObjectWriter jsonObjectWriter;

    protected long startTime;
    protected long endTime;
    protected QueryResponse queryResponse;

    protected static Logger logger;

    /**
     * Loading properties file just one time to be more efficient. All methods
     * will check parameters so to avoid extra operations this config can load
     * versions and species
     */
    protected static CellBaseConfiguration cellBaseConfiguration; //= new CellBaseConfiguration()

    /**
     * DBAdaptorFactory creation, this object can be initialize with an
     * HibernateDBAdaptorFactory or an HBaseDBAdaptorFactory. This object is a
     * factory for creating adaptors like GeneDBAdaptor
     */
    protected static DBAdaptorFactory dbAdaptorFactory;

    static {
        logger = LoggerFactory.getLogger("org.opencb.cellbase.server.ws.GenericRestWSServer");
        logger.info("Static block, creating MongoDBAdapatorFactory");
        try {
            if (System.getenv("CELLBASE_HOME") != null) {
                logger.info("Loading configuration from '{}'", System.getenv("CELLBASE_HOME")+"/configuration.json");
                cellBaseConfiguration = CellBaseConfiguration
                        .load(new FileInputStream(new File(System.getenv("CELLBASE_HOME") + "/configuration.json")));
            } else {
                logger.info("Loading configuration from '{}'",
                        CellBaseConfiguration.class.getClassLoader().getResourceAsStream("configuration.json").toString());
                cellBaseConfiguration = CellBaseConfiguration
                        .load(CellBaseConfiguration.class.getClassLoader().getResourceAsStream("configuration.json"));
            }

            // If Configuration has been loaded we can create the DBAdaptorFactory
            dbAdaptorFactory = new MongoDBAdaptorFactory(cellBaseConfiguration);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        jsonObjectMapper = new ObjectMapper();
        jsonObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        jsonObjectWriter = jsonObjectMapper.writer();
    }

    public GenericRestWSServer(@PathParam("version") String version, @PathParam("species") String species,
                               @Context UriInfo uriInfo, @Context HttpServletRequest hsr) throws VersionException, IOException {
        this.version = version;
        this.species = species;
        this.uriInfo = uriInfo;
        this.httpServletRequest = hsr;

        logger.debug("Executing GenericRestWSServer constructor");
        init(version, species, uriInfo);
    }

    protected void init(String version, String species, UriInfo uriInfo) throws VersionException, IOException {
        startTime = System.currentTimeMillis();
        queryResponse = new QueryResponse();

        // mediaType = MediaType.valueOf("text/plain");
        queryOptions = new QueryOptions();

        try {
            checkParams();
        } catch (SpeciesException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void checkParams() throws VersionException, SpeciesException {
        if (version == null) {
            throw new VersionException("Version not valid: '" + version + "'");
        }
        if (species == null) {
            throw new SpeciesException("Species not valid: '" + species + "'");
        }

        /**
         * Check version parameter, must be: v1, v2, ... If 'latest' then is
         * converted to appropriate version
         */
        if (version.equalsIgnoreCase("latest")) {
            version = cellBaseConfiguration.getVersion();
            logger.info("Version 'latest' detected, setting version parameter to '{}'", version);
        }

        if (!cellBaseConfiguration.getVersion().equalsIgnoreCase(this.version)) {
            logger.error("Version '{}' does not match configuration '{}'", this.version, cellBaseConfiguration.getVersion());
            throw new VersionException("Version not valid: '" + version + "'");
        }

//        parseCommonQueryParameters(uriInfo.getQueryParameters());
        MultivaluedMap<String, String> multivaluedMap = uriInfo.getQueryParameters();
        queryOptions.put("metadata", (multivaluedMap.get("metadata") != null) ? multivaluedMap.get("metadata").get(0).equals("true") : true);

//        System.out.println("multivaluedMap.get(\"exclude\") = " + multivaluedMap.get("exclude"));
//        queryOptions.put("exclude", (exclude != null && !exclude.equals(""))
//                ? new LinkedList<>(Splitter.on(",").splitToList(exclude))
//                : Splitter.on(",").splitToList(multivaluedMap.get("exclude").get(0).toString()));
//        queryOptions.put("include", (include != null && !include.equals(""))
//                ? new LinkedList<>(Splitter.on(",").splitToList(include))
//                : Splitter.on(",").splitToList(multivaluedMap.get("include").get(0).toString()));
        if(exclude != null && !exclude.equals("")) {
            queryOptions.put("exclude", new LinkedList<>(Splitter.on(",").splitToList(exclude)));
        } else {
            queryOptions.put("exclude", (multivaluedMap.get("exclude") != null)
                    ? Splitter.on(",").splitToList(multivaluedMap.get("exclude").get(0))
                    : null);
        }

        if(include != null && !include.equals("")) {
            queryOptions.put("include", new LinkedList<>(Splitter.on(",").splitToList(include)));
        } else {
            queryOptions.put("include", (multivaluedMap.get("include") != null)
                    ? Splitter.on(",").splitToList(multivaluedMap.get("include").get(0))
                    : null);
        }

        queryOptions.put("limit", (limit > 0) ? limit : -1);
        queryOptions.put("skip", (skip > 0) ? skip : -1);
        queryOptions.put("count", (count != null && !count.equals("")) ? Boolean.parseBoolean(count) : false);

        outputFormat = (outputFormat != null && !outputFormat.equals("")) ? outputFormat : "json";
        filename = (multivaluedMap.get("filename") != null) ? multivaluedMap.get("filename").get(0) : "result";

        // Now we add all the others QueryParams in the URL
        for (Map.Entry<String, List<String>> entry : multivaluedMap.entrySet()) {
            if(!queryOptions.containsKey(entry.getKey())) {
                logger.info("Adding '{}' to queryOptions", entry);
                queryOptions.put(entry.getKey(), entry.getValue().get(0));
            }
        }
    }

    @Override
    public Response stats() {
        return null;
    }

    @GET
    @Path("/help")
    public Response help() {
        return createOkResponse("No help available");
    }

    @GET
    public Response defaultMethod() {
        switch (species) {
            case "species":
                return getAllSpecies();
            case "echo":
                return createStringResponse("Status active");
        }
        return createOkResponse("Not valid option");
    }

    @GET
    @Path("/info")
    @ApiOperation(httpMethod = "GET", value = "Retrieves genome info for current species", response = QueryResponse.class)
    public Response getSpeciesInfo() {
        try {
            GenomeDBAdaptor genomeDBAdaptor = dbAdaptorFactory.getGenomeDBAdaptor(species, this.assembly);
            return createOkResponse(genomeDBAdaptor.getGenomeInfo(queryOptions));
        } catch (com.mongodb.MongoException e) {
            e.printStackTrace();
            return null;
        }
    }

    @GET
    @Path("/version")
    public Response getVersion() {
        return createOkResponse(cellBaseConfiguration.getDownload(), MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * Auxiliar methods
     */
    @GET
    @Path("/{species}/{category}")
    public Response getCategory(@PathParam("species") String species, @PathParam("category") String category) {
        if (isSpecieAvailable(species)) {
            if ("feature".equalsIgnoreCase(category)) {
                return createOkResponse("exon\ngene\nkaryotype\nprotein\nsnp\ntranscript");
            }
            if ("genomic".equalsIgnoreCase(category)) {
                return createOkResponse("position\nregion\nvariant");
            }
            if ("network".equalsIgnoreCase(category)) {
                return createOkResponse("pathway");
            }
            if ("regulatory".equalsIgnoreCase(category)) {
                return createOkResponse("mirna_gene\nmirna_mature\ntf");
            }
            return createOkResponse("feature\ngenomic\nnetwork\nregulatory");
        } else {
            return getAllSpecies();
        }
    }

    @GET
    @Path("/{species}/{category}/{subcategory}")
    public Response getSubcategory(@PathParam("species") String species, @PathParam("category") String category,
                                   @PathParam("subcategory") String subcategory) {
        return getCategory(species, category);
    }


    private Response getAllSpecies() {
        try {
//            List<CellBaseConfiguration.SpeciesProperties.Species> speciesList = cellBaseConfiguration.getAllSpecies();
//            return createOkResponse(jsonObjectWriter.writeValueAsString(speciesList), MediaType.APPLICATION_JSON_TYPE);
            QueryResult queryResult = new QueryResult();
            queryResult.setId("species");
            queryResult.setDbTime(0);
            queryResult.setResult(Arrays.asList(cellBaseConfiguration.getSpecies()));
            return createOkResponse(queryResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    protected Response createModelResponse(Class clazz) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
            mapper.acceptJsonFormatVisitor(mapper.constructType(clazz), visitor);
            JsonSchema jsonSchema = visitor.finalSchema();

            return createOkResponse(jsonSchema);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    protected Response createErrorResponse(Exception e) {
        // First we print the exception in Server logs
        e.printStackTrace();

        // Now we prepare the response to client
        queryResponse = new QueryResponse();
        queryResponse.setTime(new Long(System.currentTimeMillis() - startTime).intValue());
        queryResponse.setApiVersion(version);
        queryResponse.setQueryOptions(queryOptions);
        queryResponse.setError(e.toString());

        QueryResult<ObjectMap> result = new QueryResult();
        result.setWarningMsg("Future errors will ONLY be shown in the QueryResponse body");
        result.setErrorMsg("DEPRECATED: " + e.toString());
        queryResponse.setResponse(Arrays.asList(result));

        return Response
                .fromResponse(createJsonResponse(queryResponse))
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .build();
    }

    protected Response createErrorResponse(String method, String errorMessage) {
        try {
            return buildResponse(Response.ok(jsonObjectWriter.writeValueAsString(new HashMap<>().put("[ERROR] " + method, errorMessage)), MediaType.APPLICATION_JSON_TYPE));
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    protected Response createOkResponse(Object obj) {
        queryResponse = new QueryResponse();
        queryResponse.setTime(new Long(System.currentTimeMillis() - startTime).intValue());
        queryResponse.setApiVersion(version);
        queryResponse.setQueryOptions(queryOptions);

        // Guarantee that the QueryResponse object contains a list of results
        List list;
        if (obj instanceof List) {
            list = (List) obj;
        } else {
            list = new ArrayList(1);
            list.add(obj);
        }
        queryResponse.setResponse(list);

        return createJsonResponse(queryResponse);
    }

    protected Response createOkResponse(Object obj, MediaType mediaType) {
        return buildResponse(Response.ok(obj, mediaType));
    }

    protected Response createOkResponse(Object obj, MediaType mediaType, String fileName) {
        return buildResponse(Response.ok(obj, mediaType).header("content-disposition", "attachment; filename =" + fileName));
    }

    protected Response createStringResponse(String str) {
        return buildResponse(Response.ok(str));
    }

    protected Response createJsonResponse(QueryResponse queryResponse) {
        try {
            return buildResponse(Response.ok(jsonObjectWriter.writeValueAsString(queryResponse), MediaType.APPLICATION_JSON_TYPE));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("Error parsing queryResponse object");
            return createErrorResponse("", "Error parsing QueryResponse object:\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    private Response buildResponse(ResponseBuilder responseBuilder) {
        return responseBuilder
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "x-requested-with, content-type")
                .build();
    }




    /**
     * TO DELETE
     */
    @Deprecated
    protected Response generateResponse(String queryString, List features) throws IOException {
        return createOkResponse("TODO: generateResponse is drepecated");
    }

    @Deprecated
    protected Response generateResponse(String queryString, String headerTag, List features) throws IOException {
        return createOkResponse("TODO: generateResponse is drepecated");
    }

    @Deprecated
    private boolean isSpecieAvailable(String species) {
        List<CellBaseConfiguration.SpeciesProperties.Species> speciesList = cellBaseConfiguration.getAllSpecies();
        for (int i = 0; i < speciesList.size(); i++) {
            // This only allows to show the information if species is in 3
            // letters format
            if (species.equalsIgnoreCase(speciesList.get(i).getId())) {
                return true;
            }
        }
        return false;
    }

    //    @Deprecated
//    protected Response createJsonResponse(List<QueryResult> obj) {
//        endTime = System.currentTimeMillis() - startTime;
//        queryResponse.setTime((int) endTime);
//        queryResponse.setApiVersion(version);
//        queryResponse.setQueryOptions(queryOptions);
//        queryResponse.setResponse(obj);
//
////        queryResponse.put("species", species);
////        queryResponse.put("queryOptions", queryOptions);
////        queryResponse.put("response", obj);
//
//        try {
//            return buildResponse(Response.ok(jsonObjectWriter.writeValueAsString(queryResponse), MediaType.APPLICATION_JSON_TYPE));
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//            logger.error("Error parsing queryResponse object");
//            return null;
//        }
//    }


//    @Deprecated
//    protected Response createErrorResponse(Object o) {
//        String objMsg = o.toString();
//        if (objMsg.startsWith("ERROR:")) {
//            return buildResponse(Response.ok("" + o));
//        } else {
//            return buildResponse(Response.ok("ERROR: " + o));
//        }
//    }

//    @Deprecated
//    protected Response createOkResponse(String message) {
//        return buildResponse(Response.ok(message));
//    }

//    @Deprecated
//    protected Response createOkResponse(QueryResult queryResult) {
//        return createOkResponse(Arrays.asList(queryResult));
//    }

//    @Deprecated
//    protected Response createOkResponse(List<QueryResult> queryResults) {
//        switch (outputFormat.toLowerCase()) {
//            case "json":
//                return createJsonResponse(queryResults);
//            case "xml":
//                return createOkResponse(queryResults, MediaType.APPLICATION_XML_TYPE);
//            default:
//                return buildResponse(Response.ok(queryResults));
//        }
//    }

    //    protected Response createResponse(String response, MediaType mediaType) throws IOException {
//        logger.debug("CellBase - CreateResponse, QueryParams: FileFormat => " + fileFormat + ", OutputFormat => " + outputFormat + ", Compress => " + outputCompress);
//        logger.debug("CellBase - CreateResponse, Inferred media type: " + mediaType.toString());
//        logger.debug("CellBase - CreateResponse, Response: " + ((response.length() > 50) ? response.substring(0, 49) + "..." : response));
//
//        if (fileFormat == null || fileFormat.equalsIgnoreCase("")) {
//            if (outputCompress != null && outputCompress.equalsIgnoreCase("true")
//                    && !outputFormat.equalsIgnoreCase("jsonp") && !outputFormat.equalsIgnoreCase("jsontext")) {
//                response = Arrays.toString(StringUtils.gzipToBytes(response)).replace(" ", "");
//            }
//        } else {
//            mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
//            logger.debug("\t\t - Creating byte stream ");
//
//            if (outputCompress != null && outputCompress.equalsIgnoreCase("true")) {
//                OutputStream bos = new ByteArrayOutputStream();
//                bos.write(response.getBytes());
//
//                ZipOutputStream zipstream = new ZipOutputStream(bos);
//                zipstream.setLevel(9);
//
//                logger.debug("CellBase - CreateResponse, zipping... Final media Type: " + mediaType.toString());
//
//                return this.createOkResponse(zipstream, mediaType, filename + ".zip");
//
//            } else {
//                if (fileFormat.equalsIgnoreCase("xml")) {
//                    // mediaType = MediaType.valueOf("application/xml");
//                }
//
//                if (fileFormat.equalsIgnoreCase("excel")) {
//                    // mediaType =
//                    // MediaType.valueOf("application/vnd.ms-excel");
//                }
//                if (fileFormat.equalsIgnoreCase("txt") || fileFormat.equalsIgnoreCase("text")) {
//                    logger.debug("\t\t - text File ");
//
//                    byte[] streamResponse = response.getBytes();
//                    // return Response.ok(streamResponse,
//                    // mediaType).header("content-disposition","attachment; filename = "+
//                    // filename + ".txt").build();
//                    return this.createOkResponse(streamResponse, mediaType, filename + ".txt");
//                }
//            }
//        }
//        logger.debug("CellBase - CreateResponse, Final media Type: " + mediaType.toString());
//        // return Response.ok(response, mediaType).build();
//        return this.createOkResponse(response, mediaType);
//    }

}
