package com.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.neo4j.driver.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DoctorsController {

    private final Driver driver;

    public DoctorsController(Driver driver) {
        this.driver = driver;
    }

    public static List<String> resultToString(Result result, List<String> cols,
                                              List<String> rels) {
        List<String> results = new ArrayList<>();
        for (Record r : result.list()) {
            StringBuffer s = new StringBuffer();
            for (String col : cols) {
                s.append(r.get(col).asNode().asMap().toString());
            }
            for (String rel : rels) {
                s.append(r.get(rel).asRelationship().asMap().toString());
            }
            results.add(s.toString());
        }
        return results;
    }

    // Find a doctors timings.
    @RequestMapping(path = "/docs/hours/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getDocTimings(@PathVariable(value = "id") String name) {
        try (Session session = driver.session()) {
            List<String> cols = new ArrayList<>();
            cols.add("l");
            cols.add("p");

            List<String> rels = new ArrayList<>();
            rels.add("loc");
            return resultToString(session.run("MATCH(l:Location)" +
                            "<-[loc:LOCATED_AT]-(p:Person) WHERE p.name ='"
                            + name+"' RETURN l,loc,p"),
                    cols, rels);
        }
    }

    // Find doctors in network X.
    @RequestMapping(path = "/docs/network/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getDocsInNetwork(@PathVariable(value = "id") String network) {

        try (Session session = driver.session()) {
            List<String> cols = new ArrayList<>();
            cols.add("p");

            List<String> rels = new ArrayList<>();
            rels.add("net");
            return resultToString(session.run("MATCH(i:Insurance)" +
                    "<-[net:IN_NETWORK]-" +
                    "(p:Person) WHERE i.name='" + network + "'RETURN net,p"),
                    cols, rels);
        }
    }

    // Find doctors with copay less than X in Y network.
    @RequestMapping(path = "/docs/network/{nid}/{cid}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getDocsInNetworkWithXCopay(@PathVariable(value = "nid"
    ) String network, @PathVariable(value = "cid") String copay) {
        try (Session session = driver.session()) {
            List<String> cols = new ArrayList<>();
            cols.add("p");

            List<String> rels = new ArrayList<>();
            rels.add("net");
            return resultToString(session.run("MATCH(i:Insurance)" +
                    "<-[net:IN_NETWORK]-" +
                    "(p:Person) WHERE i.name='" + network + "' and net.copay " +
                    "< " + copay + " RETURN net,p"), cols, rels);
        }
    }

    // Find doctors in a city of a particular specialization
    @RequestMapping(path = "/docs/find/{sid}/{cid}", produces =
            MediaType.APPLICATION_JSON_VALUE)
    public List<String> getDocsBySpecializationAndCity(@PathVariable(value =
            "sid") String special, @PathVariable(value = "cid") String city) {
        try (Session session = driver.session()) {
            return session.run("MATCH(l:Location)<-[:LOCATED_AT]-(p:Person)" +
                    "-[:SPECIALIZES_IN]-(s:Speciality) WHERE l.name='" + city +
                    "'and s.name ='" + special + "' RETURN p")
                    .list(r -> r.get("p").asNode().asMap().toString());
        }
    }

    // Recommend doctors in the same specialization but with a lower copay.
    @RequestMapping(path = "/docs/lowercopay/{name}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getDocsWithLowerCopay(@PathVariable(value = "name") String name) {

        try (Session session = driver.session()) {
            return session.run("Match(s:Speciality)<-[:SPECIALIZES_IN]-" +
                    "(p:Person) WHERE  p.name='" + name + "'Match(s)" +
                    "<-[:SPECIALIZES_IN]-(prsn:Person) where prsn.charge < p" +
                    ".charge RETURN prsn")
                    .list(r -> r.get("prsn").asNode().asMap().toString());
        }
    }

    // Recommend doctors in the same city and specialization.
    @RequestMapping(path = "/docs/similar/{name}", produces =
            MediaType.APPLICATION_JSON_VALUE)
    public List<String> getSimilarDocs(@PathVariable(value = "name") String name) {

        try (Session session = driver.session()) {
            List<String> cols = new ArrayList<>();
            cols.add("prsn");
            return resultToString(session.run("Match" +
                    "(s:Speciality)<-[:SPECIALIZES_IN]-(p:Person) WHERE  p" +
                    ".name='" + name + "'Match(s)<-[:SPECIALIZES_IN]-" +
                    "(prsn:Person)" +
                    " Match(l:Location)<-[:LOCATED_AT]-(prsn) WHERE prsn" +
                    ".name<>\'" + name + "' and l.name =\"Santa Clara\"  " +
                    "RETURN prsn"), cols, Collections.emptyList());

        }
    }
}
