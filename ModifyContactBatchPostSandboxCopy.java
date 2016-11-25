global class ModifyContactBatchPostSandboxCopy implements SandboxPostCopy{ 
    /**
     *  @desc Implementing runApexClass Method to execute the batch
     */ 
    global void runApexClass(SandboxContext context) {
    // Pass User Email Address who clicked on refresh button in Production                   
        ModifyContactRecords_Batch con = new ModifyContactRecords_Batch(Userinfo.getUserEmail());
        database.executeBatch(con);
      
    }
}
