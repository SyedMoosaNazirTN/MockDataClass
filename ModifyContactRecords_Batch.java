global class ModifyContactRecords_Batch implements Database.Batchable<SObject>, Database.Stateful {
/*------------------------------------------------------------------------
Author:        Rohit Kumar  
Company:       Salesforce
Description:   A batch class created to mask contact field values . This Batch would be executed only if Organization is Sandbox or is invoed due to test class.
               This class is executed from VF "ModifyContactPage"
               Responsible for:
               1 - Marking different fields in Contact to dummy values
               2 - Field being masked would be selected from a Field Set "ContactFieldsForUpdate" on Contact Object
               3- Logging the error into Application Log

Test Class:    ModifyContactController_Test
History
<Date>            <Authors Name>    <Brief Description of Change>
--------------------------------------------------------------------------*/    
    
    global string emailAddress ; // Variable to hold email address
    global String jobResult = 'Success'; //keeps track of any errors during job execution
    global DateTime jobStartDateTime;    //Keeps track of start time for the job
    
    /**
     * @desc Constructor to initialize email address
     */ 
    global ModifyContactRecords_Batch(String emailValue){
        //initialize the email address passed
        this.emailAddress = emailValue;
    }
    
   /**
    * @desc Execute the start method
    */ 
    global Database.QueryLocator start(Database.BatchableContext context) {
        
        jobStartDateTime = DateTime.now();
        
        return Database.getQueryLocator(fetchContactSOQL());
    } 
    
   /**
    * @desc This method would call updateContactRecords method to modify contact records
    */ 
    global void execute(Database.BatchableContext context, List<Contact> contactRecordList ) {
        // Exectue this batch only if the instance is Sandbox or if this is executed due to test class
        Organization org = [select Id, IsSandbox from Organization Limit 1];
        if(org.IsSandbox || Test.isRunningTest()){
            list<Contact> modifiedContactRecords = updateContactRecords(contactRecordList);
            try{
                System.Debug( 'NBN: -> BatchRecords Size' + modifiedContactRecords.size() );
                update modifiedContactRecords;
            }catch(Exception ex){
                // Log the exception
                String errorMsg = 'Failed to Mask Contact Record  ' + '/n' + ex.getMessage();
                GlobalUtility.logMessage('Error', 'ModifyContactRecords_Batch',  'execute()', context.getJobId(), 'AsyncApexJob Id', errorMsg, '',  ex, 0); 
                jobResult = 'Fail';
            
            }
        }
    }
    /**
     * @desc Method to send email once batch job is completed
     */ 
    global void finish(Database.BatchableContext ctx) {
        
        String logLevel = 'Info';
        long jobExecTime = 0;
        String jobMsg;
        
        jobExecTime = DateTime.now().getTime() - jobStartDateTime.getTime();
         if ( jobResult == 'Success' ) {
            jobMsg = 'ModifyContactRecord_Batch Job Successful' +'\n';
        }
        else {
            jobMsg = 'ModifyContactRecord_Batch Job Errored' +'\n';
            logLevel = 'Error';
        }
        
         //Below code will fetch the job Id
          AsyncApexJob jobInfo = [Select a.TotalJobItems, 
                                   a.Status, 
                                   a.NumberOfErrors,
                                   a.JobType, 
                                   a.JobItemsProcessed, 
                                   a.ExtendedStatus, 
                                   a.CreatedById, 
                                   a.CompletedDate 
                            From AsyncApexJob a 
                            WHERE id = :ctx.getJobId()];//get the job Id
          
        
        jobMsg = jobMsg + '============================================' +'\n';
        jobMsg = jobMsg + 'Job #: ' + jobInfo.Id +  +'\n';
        jobMsg = jobMsg + 'Start Time: ' + jobStartDateTime + '\n';
        jobMsg = jobMsg + 'End Time: ' + DateTime.now() + '\n';
        jobMsg = jobMsg + 'Execution Time: ' + jobExecTime + '\n';
        jobMsg = jobMsg + 'Status: ' + jobInfo.Status +'\n';
        jobMsg = jobMsg + 'Items Processed: ' + jobInfo.JobItemsProcessed +'\n';
        jobMsg = jobMsg + 'No. of Errors: ' + jobInfo.NumberOfErrors +'\n';
        jobMsg = jobMsg + '============================================';

        // Log the email message being sent in Application Log
        GlobalUtility.logMessage( logLevel,'ModifyContactRecords_Batch', 'finish()', jobInfo.Id, 'AsyncApexJob Id',  jobmsg, '', null, jobExecTime );
                                   
        Messaging.SingleEmailMessage mail = new Messaging.SingleEmailMessage();
         
         
          System.debug('NBN: -> Email Address Of the User' + emailAddress);
          
          //below code will send an email to User about the status
          if(String.isNotBlank(emailAddress)){
               mail.setToAddresses(new String[] {emailAddress});
               mail.setReplyTo('noreply@salesforce.com');
               mail.setSenderDisplayName('Apex Batch Processing Module');
               mail.setSubject('Batch Processing for Contact Mask update'+jobInfo.Status);
               mail.setPlainTextBody(jobMsg);
               if(!Test.isRunningTest()) // This is added to avoid mails being sent while execting test class
               Messaging.sendEmail(new Messaging.Singleemailmessage [] {mail});
          }
    }
    
    /**
     * @desc Method to update Field Values to a dummy value
     * @param list of Contact Records
     */
     private static list<Contact> updateContactRecords(list<Contact> contactRecordsList){
       
         for(Contact contactRecord : contactRecordsList){
             // Fetch fields from field set
             for(Schema.FieldSetMember f : SObjectType.Contact.FieldSets.ContactFieldsForUpdate.getFields()){
                 String nmber = Math.random() + '' + System.now().getTime();
                 nmber = nmber.substring(0, 8);
                 if(f.getType() == Schema.DisplayType.Date){ // For field of type date
                   contactRecord.put(f.getFieldPath(),System.today().addDays(-200)); 
                 }else if(f.getType() == Schema.DisplayType.Email){ //For field of type email
                    contactRecord.put(f.getFieldPath(),('test'+nmber+'@dummy.com')); 
                 }else if(f.getType() == Schema.DisplayType.Boolean){ // For field of type boolean
                     contactRecord.put(f.getFieldPath(),false);
                 }else if(f.getType() == Schema.DisplayType.Phone){ // For field of type phone
                    
                     contactRecord.put(f.getFieldPath(),'12345');
                 }else if(f.getType() == Schema.DisplayType.Double){ // For decimal / floating  field
                     contactRecord.put(f.getFieldPath(),5.00);
                    
                 }else if(f.getType() == Schema.DisplayType.Datetime){ // For field of type datetime
                     contactRecord.put(f.getFieldPath(),System.now());
                 }else if(f.getType() == Schema.DisplayType.Integer){ // For field of type integer 
                     
                     contactRecord.put(f.getFieldPath(),5);
                 }else if(f.getFieldPath() != 'Name' && f.getFieldPath() != 'MailingGeocodeAccuracy'){ 
                     // Adding this condition to exclue name and MailingGeocodeAccuracy field as in field set selection of just last name , first name 
                     // and specific fields are not possible
                     contactRecord.put(f.getFieldPath(),('test'+nmber));
                 }
                 
             }
         }
         
         return contactRecordsList;
     }
    
    /**
     *  @desc This method would pull field schema from Field Set defined
              This is required to avoid hardcoding of any fields name
			  Fields mentioned in ContactFieldSet
				Department
				Asst. Phone
				Mailing Address
				Mailing Address
				Other Phone
				Title
				Phone
				Assistant Email
				Mailing Address
				Mailing Address
				Description
				Assistant Phone
				Department
				Job Title
				Home Phone
				Birthdate
				Areas of Interest
				Fax
				Mailing Address
				Mailing Address
				Name
				Name
				Email
				Mobile
				Name	
				Name
     */
    private static string fetchContactSOQL(){
       String query = 'SELECT ';
              for(Schema.FieldSetMember f : SObjectType.Contact.FieldSets.ContactFieldsForUpdate.getFields() ) {
                     query += f.getFieldPath() + ', ';
                 }
                 query += 'Id FROM Contact';
                 return query;
        }
}
