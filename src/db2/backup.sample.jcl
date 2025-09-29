//DB2MLCP JOB (ACCT),'DB2 FULL COPY LISTDEF',CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//*
//* ====================================================================
//*  This job first quiesces a DB2 database to establish a point
//*  of consistency, and then uses a LISTDEF to perform a full
//*  image copy backup (SHRLEVEL CHANGE) of all tablespaces and
//*  indexspaces within that database.
//* ====================================================================
//*
//* ====== Common Symbolic Parameter Definitions ======
//         SET HLQ='MYDB'                  /* High-Level Qualifier for datasets   */
//         SET SUBSYS='DSN1'               /* DB2 subsystem ID                    */
//         SET DBNAME='LAB01'              /* Database name                       */
//         SET UNIT='SYSDA'                /* Storage unit (e.g., SYSDA or TAPE)  */
//         SET PRI=50,SEC=10               /* Space allocation (in cylinders)     */
//         SET UID='BKUP'                  /* Generic Unique ID for the utility   */
//         SET TAG='Y20250929.T120000'     /* Timestamp tag for the backup        */
//         SET PARALLEL=2                  /* Degree of parallelism for the copy  */
//         SET COPY1='SYSCOPY'             /* DD name for the primary copy        */
//*
//*------------------------------------------------------------------*
//* Step 1: QUIESCE - Create a point-of-consistency for DB &DBNAME
//*------------------------------------------------------------------*
//QUIESCE  EXEC PGM=DSNUTILB,PARM='&SUBSYS,&UID..QUIESCE'
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
  QUIESCE DATABASE &DBNAME WRITE(YES)
/*
//*
//*------------------------------------------------------------------*
//* Step 2: COPYLIST - Back up all objects defined in the LISTDEF
//* This step only runs if the QUIESCE step is successful (RC=0).
//*------------------------------------------------------------------*
//COPYLIST EXEC PGM=DSNUTILB,PARM='&SUBSYS,&UID..COPYFULL',
//         COND=(0,NE,QUIESCE)
//SYSUT1   DD UNIT=&UNIT,SPACE=(CYL,(10,10))
//SYSPRINT DD SYSOUT=*
//SYSUDUMP DD SYSOUT=*
//SYSIN    DD *
  /*----------------------------------------------------------------*/
  /* Define a list named L_DB_BACKUP to include all tablespaces   */
  /* and indexspaces in the specified database.                   */
  /*----------------------------------------------------------------*/
  LISTDEF L_DB_BACKUP
    INCLUDE TABLESPACE DATABASE &DBNAME
    INCLUDE INDEXSPACE DATABASE &DBNAME

  /*----------------------------------------------------------------*/
  /* Perform a full image copy on all objects in L_DB_BACKUP.     */
  /*----------------------------------------------------------------*/
  COPY LIST L_DB_BACKUP
       COPYDDN(&COPY1)
       FULL(YES)
       SHRLEVEL(CHANGE)
       PARALLEL(&PARALLEL)
/*
//*------------------------------------------------------------------*
//* Define the backup output dataset
//*------------------------------------------------------------------*
//&COPY1   DD DSN=&HLQ..COPY.&DBNAME..FULL.&TAG,
//            DISP=(NEW,CATLG,DELETE),
//            UNIT=&UNIT,
//            SPACE=(CYL,(&PRI,&SEC)),
//            DCB=(RECFM=VB,LRECL=32760,BLKSIZE=32760)
//