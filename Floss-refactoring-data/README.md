# Floss-refactoring-data
Floss refactoring details extracted from the repositories of 45 open-source java projects

CSV's Structure

	Refactorings:
		Number: The number of the refactoring (newest to oldest);
		Commit: Hash of the commit under analysis;
		Parent: Hash of the commit's parent under analysis;
		Refactoring: Refactoring type found in the commit;
		EntityBefore: Signature of the refactored entity before the refactoring operation;
		EntityAfter: Signature of the refactored entity after the refactoring operation;
		FullDescription: RefDiff's synthesized description of the refactoring operation.
		
	Pure Refactoring Filter:
		Commit: Hash of the commit under analysis;
		PureRefactoring: code with the filtering results where...
				-1: Faield to compile all modules from the commit;
				 0: Commit contains behavior change;
				 1: No behavior change found in the commit.
	
	
	Extra Edits:
		Commit: Hash of the commit under analysis;
		ChangeType: Extra edit type found in the commit;
		ChangedEntity: Entity that was changed;
		EntityType: Type of the changed entity;
		InsideRefactoring: Boolean value idicating whether the edit was applied inside an entity refactored in the same commit;
		rootEntity: Root entity of the changed entity.
