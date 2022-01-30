require: slotfilling/slotFilling.sc
  module = sys.zb-common
  
require: localPatterns.sc
require: functions.js
  

theme: /

    state: Welcome
        q!: $regex</start>
        q!: $regex</Start>
        q!: * $hi *
        script:
            $jsapi.startSession();
        random:
            a: Здравствуйте! Я бот агентства недвижимости.
            a: Добрый день! Я бот агентства недвижимости.
        a: Я помогу вам найти новые объявления о продаже квартир с сайтов агрегаторов
        a: Хотите начать новый поиск, зайти в избранное, или получить помощь по функционалу бота?
        buttons:
            "Новый поиск"
            "Избранное"
            "Помощь"
            
    state: New_search
        q!: (* найти * / * ищу * / * поиск *)
        a: Давайте произведем поиск.
        a: Для продолжения назовите станцию кольцевой ветки московского метро, однокомнатную или двухкомнатную квартиру будем искать, а также верхнюю и нижнюю границу цены
        a: Например, однокомнатная квартира на белорусской с ценой от 15000000 до 20000000
        buttons: 
            "Отмена"
        
        state: Search || modal = true
            intent: /search_q
            script:
                $session.metro = $parseTree._metro;
                $session.rooms = $parseTree._room;
                $session.metro = capitalize($session.metro);
                $session.price1 = $parseTree._price1;
                $session.price2 = $parseTree._price2;
                
                if ($session.price1 < $session.price2){
                    $session.min_price = $session.price1
                    $session.max_price = $session.price2
                }else{
                    $session.min_price = $session.price2
                    $session.max_price = $session.price1
                }
                
                
                if ($session.rooms == "однокомнатная"){
                    $session.rooms = 1
                } else if ($session.rooms == "двухкомнатная"){
                    $session.rooms = 2
                }
                
                $session.cian_data = getCianData($session.metro, $session.rooms, $session.min_price, $session.max_price);
                
            a: Запрос {{$session.rooms}} комнатная квартира у метро {{$session.metro}} стоимостью от {{$session.min_price}} до {{$session.max_price}}
            if: $session.cian_data
                a: По вашему запросу найдено {{$session.cian_data.length}} результатов
            
            if: $session.cian_data.length == 0
                go!: /New_search
            else: $temp.cian_data.length != 0
                script:
                    $session.cur_entity = 0
                    $session.from_state = "Search"
                go!: /Entity
            
    
        state: Decline
            q: Отмена
            go!: /Welcome
    
    state: Favorites
        q!: (* избранное *)
        script:
            if ($client.favorites && $client.favorites.length != 0){
                $session.cian_data = $client.favorites
                $session.cur_entity = 0
                $session.from_state = "Favorites"
                $reactions.transition("/Entity")
            }
            else{
                $reactions.answer("Количество записей в избранном: 0")
                $reactions.transition("/New_search")
            }
            
        
    state: Help
        q!: (* *помоги* */ * *помощь* *)
        a: Для поиска работают фразы с ключевыми словами: "найти", "ищу", "поиск".
        a: Для избранного ключевые слова: "избранное".
        a: Для раздела помощь ключевые слова: "помоги", "помощь".
        a: Команды поиск, избранное, и помощь доступны из любого раздела бота.
        
        buttons:
            "Поиск и избранное"
        
        state: Search
            q: (* Поиск и избранное *)
            a: При просмотре результатов поиска и избранного доступны следующие функции:
            a: Показать следующее/предыдущее объявление. Ключевые слова: ("следующее", "дальше", "продолж")/("предыдущее", "назад", "прошлое")
            a: Повторить описание квартиры. Ключевые слова: "описание"
            a: Показать расстояние до ближайших метро. Ключевые слова: "метро"
            a: Показать цену за квартиру и квадратный метр. Ключевые слова: ("цена", "стоимость")
            a: Показать адрес. Ключевые слова: "адрес"
            a: Показать площадь Общая, Жилая, Кухня, Площадь комнат. Ключевые слова: "площадь"
            a: Показать дополнительную информацию Вид из окон, Высота потолков, Планировка, Построен, Ремонт, Санузел, Этаж). Ключевые слова: "информация"
            a: Добавить/удалить из избранного. Ключевые слова: "добавь"/"удали"
    
    state: Entity
        script:
            $session.len = 0
            if ($session.from_state === "Favorites"){
                $session.len = $client.favorites.length
            }
            if ($session.from_state === "Search"){
                $session.len = $session.cian_data.length
            }
        a: Текущее объявление {{$session.cur_entity + 1}} из {{$session.len}}
        a: {{$session.cian_data[$session.cur_entity].description.substring(0,800)}}
        buttons:
            "Следующее"
            "Предыдущее"
       
        state: Next
            q: (* следующ* * / * дальше * /* *продолжай* *)
            script:
                $session.cur_entity = mod($session.cur_entity + 1, $session.len)
            go!: /Entity
        
        state: Prev
            q: (* предыдущ* * / * назад * /* *прошлое* *)
            script:
                $session.cur_entity = mod($session.cur_entity - 1, $session.len)
            go!: /Entity
        
        state: Descrpiption
            q: (* описание *)
            a: {{$session.cian_data[$session.cur_entity].description.substring(0,800)}}
        
        state: Metro
            q: (* метро *)
            a: {{$session.cian_data[$session.cur_entity].metro}}
        
        state: Price
            q: (* цен* * / * стоимость *)
            a: Цена за квартиру {{$session.cian_data[$session.cur_entity].price}}
            a: Цена за квадратный метр {{$session.cian_data[$session.cur_entity].price_square}}
        
        state: Address
            q: (* адрес *)
            a: {{$session.cian_data[$session.cur_entity].address}}
            
        state: Area
            q: (* площадь *)
            script:
                showInfo($session.cian_data[$session.cur_entity].info, 
                ["Общая", "Жилая", "Кухня", "Площадь комнат"]);
        
        state: Info
            q: (* инфо* *)
            script:
                showInfo($session.cian_data[$session.cur_entity].info, 
                ["Вид из окон", "Высота потолков", "Планировка", "Построен", "Ремонт", "Санузел", "Этаж"]);
                
        state: AddFavorite
            q: (* добав* *)
            script:
                var $result = false
                if ($client.favorites) {
                    $client.favorites.forEach(function(item, index) {
                        if (item.link === $session.cian_data[$session.cur_entity].link){
                            $result = true
                        }
                    });
                }
                
                if ($result){
                     $reactions.answer("Запись уже есть в избранном!");
                }
                else{
                    if (!$client.favorites){
                        $client.favorites = [];
                    }
                    $client.favorites.push($session.cian_data[$session.cur_entity]);
                    $reactions.answer("Запись добавлена в избранное!");
                }
                
        
        state: RemoveFavorite
            q: (* удали* *)
            script:
                var $is_deleted = false
                if ($client.favorites){
                    for( var i = 0; i < $client.favorites.length; i++){ 
                        if ( $client.favorites[i].link === $session.cian_data[$session.cur_entity].link) { 
                            $client.favorites.splice(i, 1);
                            $is_deleted = true
                            $reactions.answer("Запись удалена из избранного!");
                        }
                    }
                }
                if (!$is_deleted){
                    $reactions.answer("Записи нет в избранном!");
                }
                
                if ($session.from_state === "Favorites" && $client.favorites.length === 0){
                    $reactions.transition("/New_search")
                }
                
                if ($session.from_state === "Favorites"){
                    $session.cian_data = $client.favorites;
                    $reactions.transition("/Favorites")
                }
            
    state: Bye
        q!: * (стоп/хватит/{(все/достаточн*) * ([на] сегодня)}/достаточн*) *
        a: До встречи!
        script:
            $jsapi.stopSession();
    
    state: CatchAll || noContext = true
        event!: noMatch
        random:
            a: Извините, такой функциональности нет.
            a: Прошу прощения, не очень вас понял.
            a: Кажется, не могу ответить на этот вопрос.
    
    state: Reset
        q!: reset   
        script:
            $client = {};
        go!: /Welcome