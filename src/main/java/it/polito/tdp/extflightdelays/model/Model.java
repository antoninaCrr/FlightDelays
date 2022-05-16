package it.polito.tdp.extflightdelays.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;

public class Model {
	private Graph<Airport,DefaultWeightedEdge> grafo; // grafo non orientato, semplice e pesato
	private ExtFlightDelaysDAO dao;
	private Map<Integer,Airport> idMap;
	
	public Model() {
		dao = new ExtFlightDelaysDAO();
		idMap = new HashMap<Integer,Airport>(); // è bene che la mappa venga creata una sola volta così che anche gli oggetto Aeroporto vengano creati una sola volta
		dao.loadAllAirports(idMap); // alla prima interazione con l'utente, riempo la Mappa con tutti gli aeroporto estratti dal DB
	}
	
	public void creaGrafo(int x) { // x è il param che userò per filtrare i vertici
		grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		//aggiungere i vertici
		Graphs.addAllVertices(this.grafo, dao.getVertici(x, idMap));
		
		//aggiungere gli archi (mediante la soluzione più lineare possibile)
		for (Rotta r : dao.getRotte(idMap)) {
			if(this.grafo.containsVertex(r.getA1()) // è buona norma controllare se i due vertici sono presenti nel grafo prima di aggiungergli eventuale arco ( i DB potrebbero essere sporchi)
					&& this.grafo.containsVertex(r.getA2())) {
				DefaultWeightedEdge edge = this.grafo.getEdge(r.getA1(),r.getA2()); // recupero l'arco tra A2 e A2
				                                                                    // se ricevo null, l'arco non c'è e quindi, lo aggiungo
				if(edge == null) {
					Graphs.addEdgeWithVertices(this.grafo, r.getA1(), r.getA2(), r.getnVoli()); // grafo, A1, A2, pesoParziale
				} else { // altrimenti, se l'arco c'è già, aggiorno il suo peso
					double pesoVecchio = this.grafo.getEdgeWeight(edge); // la libreria considera i pesi sempre come DOUBLE anche se per noi sono int
					double pesoNuovo = pesoVecchio + r.getnVoli(); 
					this.grafo.setEdgeWeight(edge, pesoNuovo); // sovrascrivo nel grafo il peso nuovo a quello vecchio
				}
			}
		}
		
	}
	
	public int nVertici() {
		return this.grafo.vertexSet().size();
	}
	
	public int nArchi() {
		return this.grafo.edgeSet().size();
	}
	
	public List<Airport> getVertici(){ // serve a restituire al Controller la lista dei vertici del grafo appena creato
		// dovrei aggiungere un controllo per verificare l'effettiva creazione del grafo
		List<Airport> vertici = new ArrayList<>(this.grafo.vertexSet());
		Collections.sort(vertici); // è buona norma ordinare con qualche criterio la lista dei miei vertici
		return vertici;
	}
	
	public List<Airport> getPercorso (Airport a1, Airport a2){
		 List<Airport> percorso = new ArrayList<>();
		 	BreadthFirstIterator<Airport,DefaultWeightedEdge> it =
				 new BreadthFirstIterator<>(this.grafo,a1); // scelto una visita in ampiezza
		 
		 Boolean trovato = false;
		 //visito il grafo
		 while(it.hasNext()) {
			 Airport visitato = it.next(); // next ritorno il nodo che visita
			 if(visitato.equals(a2))
				 trovato = true; // se durante la mia visita raggiungo l'aeroporto a2, vuol dire che c'è un collegamento tra a1 e a2
		 }
		 
		 
		 //ottengo il percorso (risalendo dalla destinazione alla sorgente)
		 if(trovato) {
			 percorso.add(a2); // aggiungengo in testa la destinazione e poi risalgo verso la sorgente
			 Airport step = it.getParent(a2); // raggiungo il padre mediante il quale la destinazione è stata scoperta
			 while (!step.equals(a1)) { // continuo ad aggiungere fin quando non raggiungo la sorgente
				 percorso.add(0,step); // aggiunta in testa in lista!
				 step = it.getParent(step);
			 }
			 
			 percorso.add(0,a1); // aggiungo la sorgente
			 return percorso; // restituisco il percorso appena calcolato
		 } else {
			 return null; // non c'è il percorso perchè la destinazione non è nella componente connessa del grafo
		 }
	}
}



















