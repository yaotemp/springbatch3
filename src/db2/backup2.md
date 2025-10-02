```o
//DB2BACK  JOB (ACCT),'DB2_OBJ_BACKUP',CLASS=A,MSGCLASS=X
//**************************************************************
//* Ensure Db2 libraries are accessible in STEPLIB
//STEPLIB   DD  DISP=SHR,DSN=DB2.DSNLOAD.LIBRARY  /* Replace with your SDSNLOAD */
//**************************************************************
//*
//* STEP 0: Define LISTDEF and TEMPLATE
//*
//STEP00    EXEC DSNUPROC,SYSTEM='DSN',UID='BACKUPID',UTPROC=''
//SYSIN     DD   *
 
*--------------------------------------------------------------*
* Define backup object list: assume we back up all objects in DBTEST
*--------------------------------------------------------------*
LISTDEF COPYLIST
     INCLUDE TABLESPACE DBTEST.*
     INCLUDE INDEXSPACE DBTEST.*   /* include indexspaces for full recovery */
     ALL                           /* also include LOB/XML related objects */

*--------------------------------------------------------------*
* Define TEMPLATE: dynamic allocation of backup datasets
* &DB., &TS., &PA. (partition/piece), &IC. (copy type), &PB. (primary/backup)
* G(+1) = next generation in GDG
*--------------------------------------------------------------*

TEMPLATE SCOPY 
         UNIT(TAPE)                                    /* store on tape */
         DSN(HLQ.DBTEST.FULL.G&IC..&PB..G(+1))         /* GDG naming convention */
         GDGLIMIT 10                                   /* keep 10 generations */
         DISP(NEW,CATLG,CATLG)

* [Optional] FlashCopy template
TEMPLATE FCOPY 
         UNIT(SYSDA) 
         DSN(HLQ.DBTEST.FC.&DB..&TS..P&PA..D&DATE..T&TIME.)
         DISP(NEW,CATLG,DELETE)
/*
 
//**************************************************************
//* STEP 1: Create consistency point (QUIESCE)
//* QUIESCE WRITE YES forces dirty pages to be written and records RBA/LRSN
//* in SYSCOPY. Recommended if using SHRLEVEL REFERENCE.
//**************************************************************
//STEP01    EXEC DSNUPROC,SYSTEM='DSN',UID='BACKUPID.QUIESCE',UTPROC=''
//SYSIN     DD   *
QUIESCE LIST COPYLIST
        WRITE YES             /* ensure dirty pages flushed to disk */
/*

//**************************************************************
//* STEP 2: Execute object backup (COPY)
//* Choose SHRLEVEL REFERENCE (safer, read-only) or SHRLEVEL CHANGE (higher availability).
//* PARALLEL improves performance.
//**************************************************************
//STEP02    EXEC DSNUPROC,SYSTEM='DSN',UID='BACKUPID.COPY',UTPROC=''
//SYSIN     DD   *

COPY LIST COPYLIST
     FULL YES                          /* full copy */
     SHRLEVEL REFERENCE                /* allow read-only, ensures consistency */
     COPYDDN(SCOPY)                    /* use TEMPLATE for dataset allocation */
     PARALLEL(4)                       /* up to 4 objects in parallel */

* [Optional: High availability with FlashCopy]
* COPY LIST COPYLIST
*      FULL YES                      
*      SHRLEVEL CHANGE                  /* allow read/write during copy */
*      FLASHCOPY CONSISTENT             /* ensure transaction consistency */
*      FCCOPYDDN(FCOPY)
*      COPYDDN(SCOPY)
*      PARALLEL(4)
/*

//**************************************************************
//* STEP 3: Delete old backup records (MODIFY RECOVERY)
//* Regularly run to clean SYSIBM.SYSCOPY and SYSLGRNX.
//**************************************************************
//STEP03    EXEC DSNUPROC,SYSTEM='DSN',UID='BACKUPID.MODIFY',UTPROC=''
//SYSIN     DD   *

MODIFY RECOVERY LIST COPYLIST          /* for all objects in LISTDEF */
                 DELETE AGE(90)        /* remove entries older than 90 days */
                 DELETEDS              /* delete related datasets as well */
                 NOCOPYPEND            /* do not set COPY-Pending if last copy deleted */
/*

```
