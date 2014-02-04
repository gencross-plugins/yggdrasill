package com.mrprez.gencross.impl.yggdrasill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.mrprez.gencross.Personnage;
import com.mrprez.gencross.PropertiesList;
import com.mrprez.gencross.Property;
import com.mrprez.gencross.formula.Formula;
import com.mrprez.gencross.formula.MalformedFormulaException;
import com.mrprez.gencross.value.StringValue;
import com.mrprez.gencross.value.Value;

public class Yggdrasill extends Personnage {
	
	
	@Override
	public void calculate() {
		super.calculate();
		if(phase.equals("archétype")){
			calculateArchetype();
			calculateBersekr();
		}else if(phase.equals("création")){
			calculateBersekr();
		}
	}
	
	private void calculateArchetype(){
		if(getProperty("Archétype").getValue().getString().equals("")){
			errors.add("Vous devez choisir un archétype");
		}
		int competenceNonChoisies = 0;
		for(Property competence : getProperty("Compétences").getSubProperties()){
			if(competence.getName().contains("Choix") && competence.getValue().getString().equals("")){
				competenceNonChoisies++;
			}
		}
		if(competenceNonChoisies>0){
			errors.add("Il vous reste "+competenceNonChoisies+" compétence(s) à choisir");
		}
		int specificationNonChoisies = 0;
		for(Property competence : getProperty("Compétences").getSubProperties()){
			if(competence.getName().contains(" à spécifier") && competence.getValue().getString().equals("")){
				specificationNonChoisies++;
			}
		}
		if(specificationNonChoisies>0){
			errors.add("Il vous reste "+competenceNonChoisies+" compétence(s) à spécifier");
		}
	}
	
	public void calculateBersekr(){
		if(getProperty("Archétype").getValue().getString().contains("Berserkr")){
			if(getProperty("Don(s)").getSubProperty("Guerrier-Fauve")==null){
				errors.add("Vous devez avoir le don Guerrier-Fauve pour être un Berserkr");
			}
		}
	}
	
	
	@Override
	public void passToNextPhase() throws Exception {
		super.passToNextPhase();
		if(phase.equals("création")){
			pointPools.get("Caractéristiques").setToEmpty(true);
			pointPools.get("Don").setToEmpty(true);
			pointPools.get("Compétences").setToEmpty(true);
			pointPools.get("Prouesses et Magie").setToEmpty(true);
			List<Property> newCompetences = new ArrayList<Property>();
			Iterator<Property> it = getProperty("Compétences").getSubProperties().iterator();
			while(it.hasNext()){
				Property oldCompetence = it.next();
				Property competence = getProperty("Compétences").getSubProperties().getDefaultProperty().clone();
				competence.setRemovable(false);
				if(oldCompetence.getName().endsWith(" à spécifier")){
					competence.setName(oldCompetence.getName().replace(" à spécifier", ""));
					competence.setSpecification(oldCompetence.getValue().getString());
				}else if(oldCompetence.getName().contains("Choix")){
					competence.setName(oldCompetence.getValue().getString());
				}else{
					competence.setName(oldCompetence.getName());
				}
				HashMap<String, String> args = new HashMap<String, String>();
				args.put("factor", "1");
				competence.getHistoryFactory().setArgs(args);
				newCompetences.add(competence);
				it.remove();
			}
			for(Property competence : newCompetences){
				getProperty("Compétences").getSubProperties().add(competence);
			}
			getProperty("Compétences").setEditableRecursivly(true);
			getProperty("Compétences").getSubProperties().setFixe(false);
			
			getProperty("Prouesses Martiales").getSubProperties().setFixe(false);
			getProperty("Magie - Sejdr").getSubProperties().setFixe(false);
			getProperty("Magie - Galdr").getSubProperties().setFixe(false);
			getProperty("Magie - Runes").getSubProperties().setFixe(false);
		}
				
	}
	
	public void addOrRemoveDon(Property property) throws MalformedFormulaException{
		if(getProperty("Don(s)#Guerrier-Fauve")!=null && getProperty("Don(s)#Initié")!=null){
			formulaManager.addFormula(new Formula("Caractéristiques secondaires#Reserve de Dés= ( #Caractéristiques principales#Corps#Vigueur# + #Caractéristiques principales#Esprit#Tenacité# + #Caractéristiques principales#Ame#Instinct# ) max ( #Caractéristiques principales#Corps#Vigueur# + #Caractéristiques principales#Esprit#Intellect# + #Caractéristiques principales#Ame#Instinct# )"));
		}else if(getProperty("Don(s)#Guerrier-Fauve")!=null){
			formulaManager.addFormula(new Formula("Caractéristiques secondaires#Reserve de Dés = #Caractéristiques principales#Corps#Vigueur# + #Caractéristiques principales#Esprit#Tenacité# + #Caractéristiques principales#Ame#Instinct#"));
		}else if(getProperty("Don(s)#Initié")!=null){
			formulaManager.addFormula(new Formula("Caractéristiques secondaires#Reserve de Dés = #Caractéristiques principales#Corps#Vigueur# + #Caractéristiques principales#Esprit#Intellect# + #Caractéristiques principales#Ame#Instinct#"));
		}else{
			formulaManager.addFormula(new Formula("Caractéristiques secondaires#Reserve de Dés= ( #Caractéristiques principales#Corps#Vigueur# + #Caractéristiques principales#Esprit#Tenacité# + #Caractéristiques principales#Ame#Instinct# ) / 2"));
		}
		formulaManager.impactModificationFor("Caractéristiques principales#Corps#Vigueur", this);
	}
	

	public boolean addFaiblesse(Property faiblesse){
		if(((Property)faiblesse.getOwner()).getSubProperties().size()>0){
			actionMessage = "Vous ne pouvez avoir plus d'une seule faiblesse";
			return false;
		}
		return true;
	}
	
	public void changeArchetype(Property archetypeProperty, Value oldValue){
		PropertiesList competencesList = getProperty("Compétences").getSubProperties();
		competencesList.getProperties().clear();
		
		String archetype = archetypeProperty.getValue().getString();
		String competenceListString = appendix.getProperty(archetype);
		String[] competenceList = competenceListString.split(";");
		for(int i=0; i<competenceList.length; i++){
			String competenceString = competenceList[i];
			Property competence = new Property(competenceString, getProperty("Compétences"));
			if(competenceString.startsWith("1 Compétence Martiale au Choix")){
				String options[] = appendix.getSubMap("competence.combat").values().toArray(new String[0]);
				competence.setOptions(options);
				competence.setValue(new StringValue(""));
				competence.setEditable(true);
			}else if(competenceString.startsWith("1 Compétence Magique au Choix")){
				List<String> options = new ArrayList<String>(3);
				if(competenceString.contains("Sejdr")){
					options.add("Sejdr");
				}
				if(competenceString.contains("Galdr")){
					options.add("Galdr");
				}
				if(competenceString.contains("Runes")){
					options.add("Runes");
				}
				competence.setOptions(options.toArray(new String[0]));
				competence.setValue(new StringValue(""));
				competence.setEditable(true);
			}else if(competenceString.contains("(Spécialisation au Choix)")){
				String competenceName = competenceString.replace("(Spécialisation au Choix)", "").trim()+" à spécifier";
				competence.setName(competenceName);
				competence.setValue(new StringValue(""));
				competence.setEditable(true);
			}else if(competenceString.contains("(")){
				String competenceName = competenceString.substring(0, competenceString.indexOf("(")).trim();
				String specification = competenceString.substring(competenceString.indexOf("(")+1,competenceString.indexOf(")")).trim();
				competence.setName(competenceName);
				competence.setSpecification(specification);
			}
			competencesList.add(competence);
		}
	}

}
