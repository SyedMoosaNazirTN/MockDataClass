global class ModifyContactBatchPostSandboxCopy implements SandboxPostCopy{
/*------------------------------------------------------------------------
Author:        Rohit Kumar  
Company:       Salesforce
Description:   A Controller class to execute "ModifyContactRecords_Batch" post sandbox copy
               Responsible for:
               1 - Execute ModifyContactRecords_Batch
              
Test Class:    ModifyContactBatchPostSandboxCopy_Test
History
<Date>            <Authors Name>    <Brief Description of Change>
--------------------------------------------------------------------------*/     
    /**
     *  @desc Implementing runApexClass Method to execute the batch
     */ 
    global void runApexClass(SandboxContext context) {
    // Pass User Email Address who clicked on refresh button in Production                   
        ModifyContactRecords_Batch con = new ModifyContactRecords_Batch(Userinfo.getUserEmail());
        database.executeBatch(con);
      
    }
}
