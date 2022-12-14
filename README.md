Trendlineapp
=============================

## How to run the application

• Make sure you have Segue JDK installed in your machine or any oracle JDK version 8 or above.

• Unzip the folder **trendlineapp**, open the codebase in your IntelliJ IDE.

• Wait for the indexing to load all resources, then navigate through **src > main > java > jclass > chart > Main**

• Hit the run button (colored green play button) in line number 10 on the left part of the workspace

## How to use Trendline application

### Step 1 

Everytime you want to use this app, you have to click the `Download Stocks` button first. 

### Step 2

Hit `Reload Data` so you will get the latest stock market informations.

### Step 3

Select company or companies in the table that you want to see in detailed graph.

### Step 4

Hit `Show Trend-Line` button.

## Buttons and Functionalities

### Download Stocks

Clicking this button will delete the contents of downloads folder. It will then get fresh stock market information from the internet (https://finance.yahoo.com/). It will populate each row of data from `US_Stock_Symbols_list.xlsx`  into the JTable.

### Reload Data

This reload data button will reload the table based on the new data of stocks fetched from `US_Stock_Symbols_list.xlsx`.

### Refresh Table

This will sort companies from JTable based on ID in ascending order.

### Search Items

You can use this to search company/companies from a specific industry of your choice.

### Clear

This button will only clear the checkbox/checkboxes ticked in the searched items panel.

### Show Trend-Line

This button will generate a graph of stock information of comapny/companies selected from the JTable.
You may save the graph from there just by right-clicking and saving it in your computer.

### Log Chart

Marking the log chart checkbox will change the info of `Y-axis` graph 

### Filter

This button will show another table. You may filter the companies in the JTable based on change in uptrend or downtrend.