# Sentinel

## Componenti del gruppo

- Andrea Cecchini (andrea.cecchini13@studio.unibo.it)
- Mattia Forti (mattia.forti2@studio.unibo.it)
- Leonardo Marcaccio (leonardo.marcaccio@studio.unibo.it)
- Deadline: 15/09/2026

## Processo di sviluppo

Il gruppo intende adottare il processo di sviluppo consigliato nel punto P8 delle regole d'esame.

## Divisione del lavoro

Nel primo meet verranno definiti i requisiti del sistema, che successivamente, nei sprint planning, saranno analizzati e suddivisi in task. L'assegnamento dei task sarà deciso secondo la metodologia Agile/SCRUM.

## Obiettivo

Implementare un sistema per modellare un magazzino automatizzato e confrontare, tramite simulazioni, diverse politiche di gestione e coordinamento di una flotta di robot.

## Descrizione del progetto

Il progetto consiste nello sviluppo di un simulatore che permetta all'utente di modellare un magazzino virtuale gestito da robot che concorrono per completare delle missioni. L'obiettivo principale è offrire un ambiente in cui testare l'infrastruttura logistica in diversi scenari operativi. Ogni scenario è configurabile tramite diversi parametri: l'utente può definire il numero e la tipologia dei robot, nonché le logiche decisionali sottostanti (algoritmi di assegnazione delle missioni, pathfinding, collision-avoidance e gestione dei guasti). Al termine di ogni simulazione, il sistema genera metriche dettagliate che consentono di confrontare le diverse configurazioni.

## Funzionalità di massima principali

- Sviluppo di un ambiente interattivo che consenta all'utente di:
  - modellare il magazzino virtuale
  - configurare i parametri dei diversi scenari
  - eseguire e visualizzare le simulazioni
- Parametrizzazione dello scenario attraverso:
  - numero di robot
  - capacità di carico dei robot
  - politiche di assegnazione delle missioni
  - politiche di calcolo dei percorsi
  - politiche di prevenzione delle collisioni
- Generazione automatica di statistiche e metriche a conclusione delle simulazioni.

## Funzionalità di massima opzionali

- Sistema di salvataggio per i layout del magazzino, le configurazioni degli scenari e i risultati delle simulazioni, al fine di consentire analisi comparative anche in sessioni di lavoro distinte.
- Gestione avanzata delle missioni:
  - supporto per l'esecuzione di missioni ricorrenti e cicliche
  - supporto per la cooperazione di più robot per il completamento di una singola missione
  - ottimizzazione delle missioni, come il raggruppamento di più missioni simili in un'unica
- Gestione avanzata dei robot:
  - gestione del consumo e della ricarica della batteria
  - gestione dei guasti