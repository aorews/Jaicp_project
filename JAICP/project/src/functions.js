function getCianData(metro, rooms, pricelow, pricehigh) {
	var response = $http.get("https://cianapi.herokuapp.com/query?metro=${metro}&rooms=${rooms}&pricelow=${pricelow}&pricehigh=${pricehigh}", {
            timeout: 10000,
            query: {
                metro: metro,
                rooms: rooms,
                pricelow: pricelow,
                pricehigh: pricehigh,
            }
        });
        
	if (!response.isOk || !response.data) {
		return false;
	}
	
    log("@@@@@@@@@@ " + toPrettyString(response));
	return response.data;
}

function mod(n, m) {
  return ((n % m) + m) % m;
}

function showInfo(dict, infos){
    infos.forEach(function(item, index) {
        if (dict[item]){
            $reactions.answer(item + ": " + dict[item]);
        }
    });
}