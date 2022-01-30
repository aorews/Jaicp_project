import pickle
import random
import re
import sys
import time
#from selenium import webdriver
from tqdm.auto import tqdm


class Flat:
    def __init__(self, link, info, info_additional, description, address, 
                    metro, price, price_square, metro_search, rooms_search
                ) -> None:

        self.link = link

        self.info = info
        self.info_additional = info_additional
        self.description = description
        self.address = address
        self.metro = metro
        self.price = price
        self.price_square = price_square

        self.metro_search = metro_search
        self.rooms_search = rooms_search


class Link:
    def __init__(self, link, type, metro = None, rooms = None):
        self.link = link
        self.type = type
        self.is_parsed = False

        if self.type == 'flat':
            self.id = int(self.link.split('/')[-2])
            self.metro = metro
            self.rooms = rooms
            
    def __eq__(self, other):
        if isinstance(other, int):
            return self.id == other
        elif  self.__class__ == other.__class__:
            return self.id == other.id

    def __hash__(self):
        return self.id


class Parser:
    def __init__(self):
        self.global_links = list()
        self.flat_links = set()
        self.flats = list()

    def get_global_links(self, rooms = [1,2]):
        # Запрос, в котором выделены станции метро на кольцевой
        query = "https://www.cian.ru/cat.php?deal_type=sale&engine_version=2&foot_min=45&metro%5B0%5D=12&metro%5B10%5D=85&metro%5B11%5D=86&metro%5B12%5D=103&metro%5B13%5D=114&metro%5B14%5D=123&metro%5B15%5D=150&metro%5B16%5D=159&metro%5B1%5D=15&metro%5B2%5D=38&metro%5B3%5D=46&metro%5B4%5D=50&metro%5B5%5D=61&metro%5B6%5D=66&metro%5B7%5D=71&metro%5B8%5D=78&metro%5B9%5D=80&offer_type=flat&only_foot=2&room2=1"
        metro_ids = re.findall('%5D=(.*?)&', query)

        for metro in metro_ids:
            for room in [1, 2]:
                link = f"https://www.cian.ru/cat.php?deal_type=sale&engine_version=2&foot_min=20&metro%5B0%5D={metro}&object_type%5B0%5D=1&offer_type=flat&only_foot=2&room{room}=1"
                self.global_links.append(Link(link, 'global'))
        return None
    
    def get_flat_links(self):
        driver = self.get_driver()

        for link in tqdm(self.global_links):
            if not link.is_parsed:
                driver.get(link.link)
                flats = driver.find_elements('xpath', "//div[@data-name='Offers']/article/div[1]/div[2]/div[1]/div/a")
                metro = driver.find_element('xpath', "//div[@data-name='Breadcrumbs']/h1").text.split('метро')[-1].strip()
                rooms = link.link[-3]
                for flat in flats:
                    self.flat_links.add(Link(flat.get_attribute('href'), 'flat', metro, rooms))
                link.is_parsed = True
                self.save()
                time.sleep(random.expovariate(0.3))
        
        driver.close()
        return None

    def get_flats(self):
        driver = self.get_driver()

        for link in tqdm(self.flat_links):
            if not link.is_parsed:
                driver.get(link.link)    
                
                info = driver.find_elements('xpath',"//div[@data-testid='object-summary-description-info-block']/div")
                info_dict = dict()
                for group in info:
                    for value, desc in [group.find_elements('xpath', 'div')]:
                        info_dict[desc.text] = value.text
                
                info_additional = driver.find_elements('xpath', "//article[@data-name='AdditionalFeaturesGroup']/ul/li")
                info_additional_dict = dict()
                for group in info_additional:
                    for type, value in [group.find_elements('xpath', 'span')]:
                        info_additional_dict[type.text] = value.text
                
                description = driver.find_element('xpath', "//p[@itemprop='description']").text
                
                address = driver.find_elements('xpath', "//address/a")
                address = ', '.join([element.text for element in address])
                
                metro_name = driver.find_elements('xpath', "//ul[contains(@class,'undergrounds')]/li/a")
                metro_distance = driver.find_elements('xpath', "//ul[contains(@class,'undergrounds')]/li/span")
                metro_dict = dict()
                for name, distance in zip(metro_name, metro_distance):
                    metro_dict[name.text] = distance.text
                
                price = driver.find_element('xpath', "//span[@itemprop='price']").text
                
                price_square = driver.find_element('xpath', "//div[contains(@class,'price_per_meter')]").text
                
                if info_dict and info_additional_dict and description and address and metro_dict and price and price_square:
                    self.flats.append(
                        Flat(
                            link.link, info_dict, info_additional_dict, description, address, metro_dict,
                            price, price_square, link.metro, link.rooms
                            )
                    )
                    link.is_parsed = True
                
                self.save()
                time.sleep(random.expovariate(0.2))
        
        driver.close()
        return None
    
    def prepare_for_api(self,):
        info_values = {k: None for flat in self.flats for k, _ in flat.info.items()}
        info_values.update({k: None for flat in self.flats for k, _ in flat.info_additional.items()})
        for flat in self.flats:
            result = dict()

            result['link'] = flat.link
            result['price'] = int(flat.price.replace(' ', '').replace('₽', ''))
            result['price_square'] = int(flat.price_square.replace(' ', '').replace('₽/м²', ''))
            result['metro'] = ', '.join([k + ' ' + v.replace('⋅ ', '').replace('мин.', 'минут') for k, v in flat.metro.items()])
            result['description'] = flat.description
            result['address'] = flat.address
            result['metro_search'] = flat.metro_search
            result['rooms_search'] = int(flat.rooms_search)

            info = info_values.copy()
            for info_part in [flat.info, flat.info_additional]:
                for k, v in info_part.items():
                    info[k] = v
            result['info'] = info

            flat.api = result
        return None

    def get_driver(self):
        chrome_options = webdriver.ChromeOptions()
        prefs = {"profile.managed_default_content_settings.images": 2}
        chrome_options.add_experimental_option("prefs", prefs)
        chrome_options.add_argument('--headless')
        driver = webdriver.Chrome(chrome_options=chrome_options)
        return driver
    
    def save(self, path = './'):
        with open(f'{path}/cian_parser.pkl', "wb" ) as f:
            pickle.dump(self,  f)
        # print("Parser saved!", file = sys.stderr)
        return None


    @staticmethod
    def load(path = './cian_parser.pkl'):
        try:
            with open(path, "rb" ) as f:
                item = pickle.load(f)
            return item
        except FileNotFoundError:
            return Parser()


if __name__ == '__main__':
    parser = Parser.load()
    try:
        if len(parser.global_links) == 0:
            parser.get_global_links()
        if any([not link.is_parsed for link in parser.global_links]):
            parser.get_flat_links()
        #if any([not link.is_parsed for link in parser.flat_links]):
            #parser.get_flats()
        parser.prepare_for_api()
    except Exception as e:
        print(e)
        parser.save()
